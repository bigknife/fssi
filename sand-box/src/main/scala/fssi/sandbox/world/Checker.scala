package fssi
package sandbox
package world

import java.io._
import java.nio.charset.Charset
import java.nio.file.{Path, Paths}

import fssi.sandbox.exception.{ContractCheckException, SandBoxEnvironmentException}
import fssi.sandbox.loader.FSSIClassLoader
import fssi.sandbox.types.SParameterType.SContext
import fssi.sandbox.types.{Method, SParameterType, SandBoxVersion}
import fssi.types.Contract
import fssi.utils.FileUtil
import org.slf4j.{Logger, LoggerFactory}
import fssi.sandbox.types._
import fssi.sandbox.config._
import scala.collection.mutable.ListBuffer

class Checker {

  private lazy val builder = new Builder

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  /** check contract class file determinism
    *
    * @param contractFile contract jar
    * @return errors when check failed
    */
  def checkDeterminism(contractFile: File): Either[ContractCheckException, Unit] = {
    logger.info(s"check contract determinism for contract file $contractFile")
    if (contractFile.exists() && contractFile.isFile) {
      val rootPath = Paths.get(contractFile.getParent, "contractRoot")
      if (rootPath.toFile.exists()) FileUtil.deleteDir(rootPath)
      rootPath.toFile.mkdirs()
      val targetPath = Paths.get(rootPath.getParent.toString, "fssi")
      try {
        better.files.File(contractFile.toPath).unzipTo(rootPath)(Charset.forName("utf-8"))
        val track = scala.collection.mutable.ListBuffer.empty[String]
        if (!targetPath.toFile.exists()) targetPath.toFile.mkdirs()
        for {
          _ <- builder.degradeClassVersion(rootPath, targetPath)
          checkClassLoader = new FSSIClassLoader(targetPath, track)
          _ <- checkContractMethod(targetPath, track, checkClassLoader)
          _ <- checkClasses(targetPath, track, checkClassLoader)
        } yield { if (targetPath.toFile.exists()) FileUtil.deleteDir(targetPath) }
      } catch {
        case t: Throwable =>
          val error = s"check contract determinism occurs error: ${t.getMessage}"
          logger.error(error, t)
          Left(ContractCheckException(Vector(error)))
      } finally {
        if (rootPath.toFile.exists()) FileUtil.deleteDir(rootPath)
        if (targetPath.toFile.exists()) FileUtil.deleteDir(targetPath)
      }
    } else {
      val error =
        s"to check determinism contract file $contractFile not found: contract must be a file assembled all of class files"
      val ex = ContractCheckException(Vector(error))
      logger.error(error, ex)
      Left(ex)
    }
  }

  def checkClasses(rootPath: Path,
                   track: ListBuffer[String],
                   checkClassLoader: FSSIClassLoader): Either[ContractCheckException, Unit] = {
    logger.info(s"check contract class at path: $rootPath")
    try {
      val classFiles =
        FileUtil.findAllFiles(rootPath).filter(file => file.getAbsolutePath.endsWith(".class"))
      classFiles.foreach { file =>
        val classFileName =
          file.getAbsolutePath.substring(rootPath.toString.length + 1).replace("/", ".")
        val className = classFileName.substring(0, classFileName.lastIndexOf(".class"))
        checkClassLoader.findClassFromClassFile(file, className, "", Array.empty)
      }
      if (track.isEmpty) Right(())
      else {
        val ex = ContractCheckException(track.toVector)
        logger.error(ex.getMessage, ex)
        Left(ex)
      }
    } catch {
      case t: Throwable =>
        val error = s"check contract class at path: $rootPath occurs error: ${t.getMessage}"
        logger.error(error, t)
        Left(ContractCheckException(Vector(error)))
    }
  }

  def checkContractMethod(
      rootPath: Path,
      track: ListBuffer[String],
      checkClassLoader: FSSIClassLoader): Either[ContractCheckException, Unit] = {
    logger.info(s"check contract method at path: $rootPath")
    import fssi.sandbox.types.Protocol._
    try {
      val contract = Paths.get(rootPath.toString, s"META-INF/$metaFileName").toFile
      if (!contract.exists() || !contract.isFile) {
        val error = s"check contract method: file $contract not found"
        logger.error(error)
        Left(ContractCheckException(Vector(error)))
      } else {
        val configReader = ConfigReader(contract)
        checkContractDescriptor(configReader.interfaces).flatMap { ms =>
          ms.foreach(m =>
            checkClassLoader.findClass(m.className, m.methodName, m.parameterTypes.map(_.`type`)))
          if (track.isEmpty) Right(())
          else {
            val ex = ContractCheckException(track.toVector)
            logger.error(ex.getMessage, ex)
            Left(ex)
          }
        }
      }
    } catch {
      case t: Throwable =>
        val error = s"check contract method at path: $rootPath occurs error: ${t.getMessage}"
        logger.error(error, t)
        Left(ContractCheckException(Vector(error)))
    }
  }

  def isProjectStructureValid(rootPath: Path): Boolean = {
    logger.info(s"check project structure at path: $rootPath")
    Paths.get(rootPath.toString, "src/main/java").toFile.isDirectory &&
    Paths.get(rootPath.toString, "src/main/resources/META-INF").toFile.isDirectory
  }

  def isResourceFilesInValid(resourcesRoot: Path, resourceFiles: Vector[File]): Vector[String] = {
    logger.info(s"check resource files validity at path: $resourcesRoot")
    import fssi.sandbox.types.Protocol._
    resourceFiles
      .map(_.getAbsolutePath)
      .foldLeft(Vector.empty[String]) { (acc, n) =>
        val fileName = n.substring(resourcesRoot.toString.length + 1)
        if (!allowedResourceFiles.contains(fileName)) acc :+ fileName
        else acc
      }
  }

  def isResourceContractFilesInvalid(resourcesRoot: Path,
                                     resourceFiles: Vector[File]): Vector[String] = {
    logger.info(s"check contract required files validity at path: $resourcesRoot")
    import fssi.sandbox.types.Protocol._
    allowedResourceFiles.foldLeft(Vector.empty[String]) { (acc, n) =>
      val existed = resourceFiles.map(_.getAbsolutePath).exists { filePath =>
        val contractFileName = filePath.substring(resourcesRoot.toString.length + 1)
        n == contractFileName
      }
      if (existed) acc
      else acc :+ n
    }
  }

  def checkContractDescriptor(contractDescriptors: Vector[MethodDescriptor])
    : Either[ContractCheckException, Vector[Method]] = {
    logger.info(s"check contract method description for descriptors file $contractDescriptors")
    val track = scala.collection.mutable.ListBuffer.empty[String]
    val methods = contractDescriptors.foldLeft(Vector.empty[Method]) { (acc, n) =>
      logger.debug(s"smart contract exposes method [${n.alias} = ${n.descriptor}]")
      n.descriptor.split("#") match {
        case Array(className, methodAssign) =>
          val leftIndex  = methodAssign.indexOf("(")
          val rightIndex = methodAssign.lastIndexOf(")")
          if (leftIndex < 0 || rightIndex < 0) {
            track += s"contract descriptor invalid: $methodAssign"; acc
          } else {
            val methodName = methodAssign.substring(0, leftIndex)
            val parameterTypes = methodAssign
              .substring(leftIndex + 1, rightIndex)
              .split(",")
              .filter(_.nonEmpty)
              .map(SParameterType(_))
            if (parameterTypes.length >= 1 && parameterTypes.head.`type`.getName == SContext.`type`.getName) {
              val method = types.Method(alias = n.alias,
                                        className = className,
                                        methodName = methodName,
                                        parameterTypes = parameterTypes)
              acc :+ method
            } else {
              track += "contract method first parameter must be type of fssi.contract.lib.Context"
              acc
            }
          }
      }
    }
    if (track.isEmpty) {
      val errors = methods.groupBy(_.alias).foldLeft(Vector.empty[String]) { (acc, n) =>
        n match {
          case (alias, mds) =>
            if (mds.size > 1)
              acc :+ s"duplicated contract method alias: $alias, found: ${mds.mkString(" , ")}"
            else acc
        }
      }
      if (errors.isEmpty) Right(methods)
      else {
        val ex = ContractCheckException(errors)
        logger.error(ex.getMessage, ex)
        Left(ex)
      }
    } else {
      val ex = ContractCheckException(track.toVector)
      logger.error(ex.getMessage, ex)
      Left(ex)
    }
  }

  private[sandbox] def isContractMethodExisted(
      method: Contract.Method,
      params: Contract.Parameter,
      methods: Vector[Method]): Either[ContractCheckException, Unit] = {
    logger.info(s"check contract method $method whether existed for params $params")
    methods.find(_.alias == method.alias) match {
      case Some(m) => isContractMethodParameterTypeMatched(params, m.parameterTypes)
      case None =>
        val error =
          s"method ${method.alias} not existed,exposed method: ${methods.mkString("\n[", "\n", "\n]")}"
        val ex = ContractCheckException(Vector(error))
        logger.error(error, ex)
        Left(ex)
    }
  }

  private def isContractMethodParameterTypeMatched(
      params: Contract.Parameter,
      parameterTypes: Array[SParameterType]): Either[ContractCheckException, Unit] = {
    logger.info(
      s"check contract method parameter type matched, params: $params, contract descriptor params types: ${parameterTypes
        .map(_.`type`.getName)
        .mkString("[", ",", "]")}")
    import fssi.types.Contract.Parameter._
    var index = 0

    def convertToSParameterType(parameter: Contract.Parameter,
                                acc: Array[SParameterType]): Array[SParameterType] = {
      index = index + 1
      parameter match {
        case PString(_) => acc :+ SParameterType.SString
        case PBool(_)   => acc :+ SParameterType.SBoolean
        case PBigDecimal(_) =>
          parameterTypes(index) match {
            case SParameterType.SBoolean => acc
            case SParameterType.SContext => acc
            case x                       => acc :+ x
          }
        case PArray(array) =>
          index = index - 1
          array.flatMap(p => convertToSParameterType(p, acc))
        case PEmpty => acc
      }
    }

    params match {
      case PArray(array) if array.length != parameterTypes.length - 1 =>
        val error =
          s"receipted method parameter amount ${array.length} is not coordinated with contract method parameter amount ${parameterTypes.length}"
        val ex = ContractCheckException(Vector(error))
        logger.error(error, ex)
        Left(ex)
      case x =>
        val receiptParameterType = SParameterType.SContext +: convertToSParameterType(x,
                                                                                      Array.empty)
        val receiptParameterTypeNames  = receiptParameterType.map(_.`type`.getName)
        val contractParameterTypeNames = parameterTypes.map(_.`type`.getName)
        if (receiptParameterTypeNames sameElements contractParameterTypeNames) Right(())
        else {
          val ex = ContractCheckException(Vector(
            s"receipted method parameter type: ${receiptParameterTypeNames.mkString("(", ",", ")")} not coordinated with contract method parameter type: ${contractParameterTypeNames
              .mkString("(", ",", ")")}"))
          logger.error(ex.getMessage, ex)
          Left(ex)
        }
    }
  }

  def isSandBoxVersionValid(inputVersion: SandBoxVersion): Boolean =
    inputVersion.lteTo(SandBoxVersion.currentVersion)

  def isSandBoxEnvironmentValid: Either[SandBoxEnvironmentException, Unit] = {
    val javaVersion           = System.getProperty("java.version")
    val jv                    = if (javaVersion.contains(".")) javaVersion.split("\\.")(1) else javaVersion
    val currentSandBoxVersion = SandBoxVersion.currentVersion
    val supportJavaHighest    = currentSandBoxVersion.supportHighestJavaVersion
    if (supportJavaHighest >= jv.toInt) Right(())
    else {
      val error =
        s"sandbox supported highest java version is $supportJavaHighest but current machine java version is $jv"
      val ex = SandBoxEnvironmentException(error)
      logger.error(error, ex)
      Left(ex)
    }
  }
}
