package fssi
package sandbox
package world

import java.io._
import java.nio.file.{Path, Paths}

import fssi.types.biz.Contract.UserContract.Parameter._
import fssi.sandbox.exception.{ContractCheckException, SandBoxEnvironmentException}
import fssi.sandbox.loader.FSSIClassLoader
import fssi.sandbox.types.SParameterType.SContext
import fssi.sandbox.types.{ContractMeta, Method, SParameterType, SandBoxVersion}
import fssi.types.biz.Contract
import fssi.utils.FileUtil

import scala.collection.mutable.ListBuffer
import fssi.sandbox.inf._
import fssi.sandbox.types.ContractMeta.{MethodDescriptor, Version}
import fssi.sandbox.types.Protocol._
import fssi.types.exception.FSSIException
import fssi.types.implicits._

class Checker extends BaseLogger {

  private lazy val builder = new Builder

  /** check contract class file determinism
    *
    * @param rootPath contract root path
    * @return errors when check failed
    */
  def checkDeterminism(rootPath: Path): Either[FSSIException, Unit] = {
    logger.info(s"check contract determinism for contract $rootPath")
    if (rootPath.toFile.exists()) {
      val targetPath = Paths.get(rootPath.getParent.toString, "fssi")
      try {
        val track = scala.collection.mutable.ListBuffer.empty[String]
        if (!targetPath.toFile.exists()) targetPath.toFile.mkdirs()
        for {
          _            <- builder.degradeClassVersion(rootPath, targetPath)
          contractMeta <- builder.buildContractMeta(rootPath)
          _            <- isContractVersionValid(contractMeta.version)
          methods      <- checkContractDescriptor(contractMeta.interfaces)
          checkClassLoader = new FSSIClassLoader(targetPath, track)
          _ <- isContractMethodExisted(checkClassLoader, methods)
          _ <- checkClasses(targetPath, track, checkClassLoader)
        } yield { if (targetPath.toFile.exists()) FileUtil.deleteDir(targetPath) }
      } catch {
        case t: Throwable =>
          val error = s"check contract determinism occurs error: ${t.getMessage}"
          logger.error(error, t)
          Left(ContractCheckException(Vector(error)))
      } finally if (targetPath.toFile.exists()) FileUtil.deleteDir(targetPath)
    } else {
      val error =
        s"to check determinism contract file $rootPath not found: contract must be a file assembled all of class files"
      val ex = ContractCheckException(Vector(error))
      logger.error(error, ex)
      Left(ex)
    }
  }

  def checkClasses(rootPath: Path,
                   track: ListBuffer[String],
                   checkClassLoader: FSSIClassLoader): Either[FSSIException, Unit] = {
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

  def isContractMethodExisted(checkClassLoader: FSSIClassLoader,
                              methods: Vector[Method]): Either[FSSIException, Unit] = {
    try {
      methods.foreach(m =>
        checkClassLoader.findClass(m.className, m.methodName, m.parameterTypes.map(_.`type`)))
      if (checkClassLoader.track.isEmpty) Right(())
      else {
        val ex = ContractCheckException(checkClassLoader.track.toVector)
        logger.error(ex.getMessage, ex)
        Left(ex)
      }
    } catch {
      case t: Throwable =>
        val error = s"check contract method occurs error: ${t.getMessage}"
        logger.error(error, t)
        Left(ContractCheckException(Vector(error)))
    }
  }

  def isProjectStructureValid(rootPath: Path): Either[FSSIException, Unit] = {
    logger.info(s"check project structure at path: $rootPath")
    val isValid = Paths.get(rootPath.toString, "src/main/java").toFile.isDirectory &&
      Paths.get(rootPath.toString, "src/main/resources/META-INF").toFile.isDirectory
    if (isValid) Right(())
    else {
      val error =
        s"$rootPath src path should have main/java and main/resources/META-INF subdirectories"
      Left(ContractCheckException(Vector(error)))
    }
  }

  def isResourceFilesInValid(resourcesRoot: Path,
                             resourceFiles: Vector[File]): Either[FSSIException, Unit] = {
    logger.info(s"check resource files validity at path: $resourcesRoot")
    import fssi.sandbox.types.Protocol._
    val invalidFiles = resourceFiles
      .map(_.getAbsolutePath)
      .foldLeft(Vector.empty[String]) { (acc, n) =>
        val fileName = n.substring(resourcesRoot.toString.length + 1)
        if (!allowedResourceFiles.contains(fileName)) acc :+ fileName
        else acc
      }
    if (invalidFiles.isEmpty) Right(())
    else {
      val error = s"src/main/resources/META-INF dir only support ${allowedResourceFiles
        .mkString("、")} files,but found ${invalidFiles.mkString(" , ")}"
      Left(ContractCheckException(Vector(error)))
    }
  }

  def isResourceContractFilesInvalid(resourcesRoot: Path,
                                     resourceFiles: Vector[File]): Either[FSSIException, Unit] = {
    logger.info(s"check contract required files validity at path: $resourcesRoot")
    import fssi.sandbox.types.Protocol._
    val invalidContractFiles = allowedResourceFiles.foldLeft(Vector.empty[String]) { (acc, n) =>
      val existed = resourceFiles.map(_.getAbsolutePath).exists { filePath =>
        val contractFileName = filePath.substring(resourcesRoot.toString.length + 1)
        n == contractFileName
      }
      if (existed) acc
      else acc :+ n
    }
    if (invalidContractFiles.isEmpty) Right(())
    else {
      val error =
        s"smart contract required files not found: ${invalidContractFiles.mkString(" , ")}"
      Left(ContractCheckException(Vector(error)))
    }
  }

  def checkContractDescriptor(
      contractDescriptors: Vector[MethodDescriptor]): Either[FSSIException, Vector[Method]] = {
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
        case _ =>
          track += "exposed method must obey rule of: 'methodAlias = qualifiedClassName#methodName(Context,arguments*)'";
          acc
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

  private[sandbox] def isContractMethodExposed(
      method: Contract.UserContract.Method,
      params: Option[Contract.UserContract.Parameter],
      methods: Vector[Method]): Either[FSSIException, Unit] = {
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
      params: Option[Contract.UserContract.Parameter],
      parameterTypes: Array[SParameterType]): Either[FSSIException, Unit] = {
    logger.info(
      s"check contract method parameter type matched, params: $params, contract descriptor params types: ${parameterTypes
        .map(_.`type`.getName)
        .mkString("[", ",", "]")}")
    var index = 0

    def convertToSParameterType(parameter: Contract.UserContract.Parameter,
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
      }
    }

    params match {
      case Some(parameter) =>
        parameter match {
          case PArray(array) if array.length != parameterTypes.length - 1 =>
            val error =
              s"receipted method parameter amount ${array.length} is not coordinated with contract method parameter amount ${parameterTypes.length}"
            val ex = ContractCheckException(Vector(error))
            logger.error(error, ex)
            Left(ex)
          case x =>
            val receiptParameterType = SParameterType.SContext +: convertToSParameterType(
              x,
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
      case None =>
        if (parameterTypes.length - 1 == 0) Right(())
        else {
          val ex = ContractCheckException(
            Vector(
              "contract exposed method required more than Context parameter but invoked with nil"))
          logger.error(ex.getMessage, ex)
          Left(ex)
        }
    }
  }

  def isSandBoxVersionValid(inputVersion: SandBoxVersion): Either[FSSIException, Unit] = {
    val valid = inputVersion.lteTo(SandBoxVersion.currentVersion)
    if (valid) Right(())
    else {
      val error =
        s"compile contract failed: sandbox version $inputVersion is greater than current version ${SandBoxVersion.currentVersion}"
      Left(ContractCheckException(Vector(error)))
    }
  }

  def isSandBoxEnvironmentValid: Either[FSSIException, Unit] = {
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

  def isContractMetaFileValid(metaFile: File): Either[FSSIException, Unit] = {
    logger.info(s"check contract meta file is valid: $metaFile")
    val configReader = ConfigReader(metaFile)
    val errors       = scala.collection.mutable.ListBuffer.empty[String]
    if (!configReader.hasConfigKey(ownerKey))
      errors += s"can't not find $ownerKey in contract meta conf file"
    if (!configReader.hasConfigKey(nameKey))
      errors += s"can't not find $nameKey in contract meta conf file"
    if (!configReader.hasConfigKey(versionKey))
      errors += s"can't not find $versionKey in contract meta conf file"
    if (!configReader.hasConfigKey(descriptionKey))
      errors += s"can't not find $descriptionKey in contract meta conf file"
    if (!configReader.hasConfigKey(interfacesKey))
      errors += s"can't not find $interfacesKey in contract meta conf file"
    if (errors.isEmpty) Right(())
    else Left(ContractCheckException(errors.toVector))
  }

  def isContractSizeValid(size: Long): Either[FSSIException, Unit] = {
    import fssi.sandbox.types.Protocol._
    val valid = size <= contractSize
    if (valid) Right(())
    else {
      val error =
        s"contract project compiled file total size $size can not exceed $contractSize bytes"
      Left(ContractCheckException(Vector(error)))
    }
  }

  def isContractOwnerValid(accountId: Array[Byte],
                           owner: ContractMeta.Owner): Either[FSSIException, Unit] = {
    val account = accountId.asBytesValue.bcBase58
    val valid   = account == owner.value
    if (valid) Right(())
    else Left(new FSSIException(s"compiler owner $owner is different with contract owner $account"))
  }

  def isContractVersionValid(version: ContractMeta.Version): Either[FSSIException, Unit] = {
    val versionOption = Contract.Version(version.value)
    if (versionOption.nonEmpty) Right(())
    else
      Left(
        new FSSIException(
          s"contract version ${version.value} is invalid must format as (maj.min.path)"))
  }
}
