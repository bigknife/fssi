package fssi
package sandbox
package world

import java.io._
import java.nio.file.{Files, Path, Paths}

import fssi.sandbox.loader.FSSIClassLoader
import fssi.sandbox.types.{Method, SParameterType}
import fssi.sandbox.visitor.DegradeClassVersionVisitor
import fssi.types.exception.ContractCheckException
import fssi.utils.FileUtil
import org.objectweb.asm.{ClassReader, ClassWriter}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer

class Checker {

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  /** check contract class file determinism
    *
    * @param contractFile contract jar
    * @return errors when check failed
    */
  def checkDeterminism(contractFile: File): Either[ContractCheckException, Unit] = {
    if (contractFile.exists() && contractFile.isFile) {
      val rootPath = Paths.get(contractFile.getParent, "contractRoot")
      if (rootPath.toFile.exists()) FileUtil.deleteDir(rootPath)
      rootPath.toFile.mkdirs()
      try {
        better.files.File(contractFile.toPath).unzipTo(rootPath)
        val track      = scala.collection.mutable.ListBuffer.empty[String]
        val targetPath = Paths.get(rootPath.getParent.toString, "fssi")
        if (!targetPath.toFile.exists()) targetPath.toFile.mkdirs()
        for {
          _ <- degradeClassVersion(rootPath, targetPath)
          checkClassLoader = new FSSIClassLoader(targetPath, track)
          _ <- checkContractMethod(rootPath, track, checkClassLoader)
          _ <- checkClasses(targetPath, track, checkClassLoader)
        } yield { if (targetPath.toFile.exists()) FileUtil.deleteDir(targetPath) }
      } catch {
        case t: Throwable => Left(ContractCheckException(Vector(t.getMessage)))
      } finally { if (rootPath.toFile.exists()) FileUtil.deleteDir(rootPath) }
    } else
      Left(
        ContractCheckException(
          Vector("contract file not found: contract must be a file assembled all of class files")))
  }

  private[world] def degradeClassVersion(rootPath: Path,
                                         targetPath: Path): Either[ContractCheckException, Unit] = {
    try {
      val metaInfoPath = Paths.get(targetPath.toString, "META-INF")
      if (metaInfoPath.toFile.exists()) metaInfoPath.toFile.delete()
      metaInfoPath.toFile.mkdirs()
      val rootResourcesPath = Paths.get(rootPath.toString, "META-INF")
      val resourcesFiles    = FileUtil.findAllFiles(rootResourcesPath)
      resourcesFiles.foreach { resourceFile =>
        val path =
          Paths.get(metaInfoPath.toString,
                    resourceFile.getAbsolutePath.substring(rootResourcesPath.toString.length + 1))
        if (path.toFile.exists() && path.toFile.isFile) path.toFile.delete()
        Files.copy(resourceFile.toPath, path)
      }
      val classFiles = FileUtil.findAllFiles(rootPath).filter(_.getAbsolutePath.endsWith(".class"))
      val buffer     = new Array[Byte](8092)
      val degradeErrors = classFiles.foldLeft(Vector.empty[String]) { (acc, classFile) =>
        if (classFile.canRead) {
          val filePath =
            Paths.get(targetPath.toString,
                      classFile.getAbsolutePath.substring(rootPath.toString.length + 1))
          val file = filePath.toFile
          if (!file.getParentFile.exists()) file.getParentFile.mkdirs()
          if (!file.exists()) file.createNewFile()
          val fileInputStream = new FileInputStream(classFile)
          val output          = new ByteArrayOutputStream()
          Iterator
            .continually(fileInputStream.read(buffer))
            .takeWhile(_ != -1)
            .foreach(read => output.write(buffer, 0, read))
          output.flush(); fileInputStream.close()
          val classBuffer  = output.toByteArray; output.close()
          val outputStream = new FileOutputStream(file, true)
          val readerConstructor = classOf[ClassReader].getDeclaredConstructor(classOf[Array[Byte]],
                                                                              classOf[Int],
                                                                              classOf[Boolean])
          val accessible = readerConstructor.isAccessible
          readerConstructor.setAccessible(true)
          val classReader = readerConstructor.newInstance(classBuffer,
                                                          Integer.valueOf(0),
                                                          java.lang.Boolean.valueOf(false))
          readerConstructor.setAccessible(accessible)
          val classWriter = new ClassWriter(classReader, 0)
          val visitor     = DegradeClassVersionVisitor(classWriter)
          classReader.accept(visitor, 0)
          val array = classWriter.toByteArray
          outputStream.write(array, 0, array.length)
          outputStream.flush(); outputStream.close(); acc
        } else acc :+ s"class file ${classFile.getAbsolutePath} can not read"
      }
      if (degradeErrors.isEmpty) Right(())
      else Left(ContractCheckException(degradeErrors))
    } catch {
      case t: Throwable => Left(ContractCheckException(Vector(t.getMessage)))
    }
  }

  private[world] def checkClasses(
      rootPath: Path,
      track: ListBuffer[String],
      checkClassLoader: FSSIClassLoader): Either[ContractCheckException, Unit] = {
    try {
      val classFiles = FileUtil.findAllFiles(rootPath).filter(_.getAbsolutePath.endsWith(".class"))
      classFiles.foreach { file =>
        val classFileName =
          file.getAbsolutePath.substring(rootPath.toString.length + 1).replace("/", ".")
        val className = classFileName.substring(0, classFileName.lastIndexOf(".class"))
        checkClassLoader.findClassFromClassFile(file, className, "", Array.empty)
      }
      if (track.isEmpty) Right(())
      else Left(ContractCheckException(track.toVector))
    } catch {
      case t: Throwable => Left(ContractCheckException(Vector(t.getMessage)))
    }
  }

  private[world] def checkContractMethod(
      rootPath: Path,
      track: ListBuffer[String],
      checkClassLoader: FSSIClassLoader): Either[ContractCheckException, Unit] = {
    import fssi.sandbox.types.Protocol._
    try {
      val contract = Paths.get(rootPath.toString, s"META-INF/$contractFileName").toFile
      if (!contract.exists() || !contract.isFile)
        Left(ContractCheckException(Vector(s"META-INF/$contractFileName file not found")))
      else {
        checkContractDescriptor(contract).flatMap { ms =>
          ms.foreach(m =>
            checkClassLoader.findClass(m.className, m.methodName, m.parameterTypes.map(_.`type`)))
          if (track.isEmpty) Right(())
          else Left(ContractCheckException(track.toVector))
        }
      }
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        Left(ContractCheckException(Vector(t.getMessage)))
    }
  }

  private[world] def isProjectStructureValid(rootPath: Path): Boolean = {
    Paths.get(rootPath.toString, "src/main/java").toFile.isDirectory &&
    Paths.get(rootPath.toString, "src/main/resources/META-INF").toFile.isDirectory
  }

  private[world] def isResourceFilesInValid(resourcesRoot: Path,
                                            resourceFiles: Vector[File]): Vector[String] = {
    import fssi.sandbox.types.Protocol._
    resourceFiles
      .map(_.getAbsolutePath)
      .foldLeft(Vector.empty[String]) { (acc, n) =>
        val fileName = n.substring(resourcesRoot.toString.length + 1)
        if (!allowedResourceFiles.contains(fileName)) acc :+ fileName
        else acc
      }
  }

  private[world] def isResourceContractFilesInvalid(resourcesRoot: Path,
                                                    resourceFiles: Vector[File]): Vector[String] = {
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

  private[world] def checkContractDescriptor(
      contractFile: File): Either[ContractCheckException, Vector[Method]] = {
    val track  = scala.collection.mutable.ListBuffer.empty[String]
    val reader = new BufferedReader(new FileReader(contractFile))
    val lines = Iterator
      .continually(reader.readLine())
      .takeWhile(_ != null)
      .foldLeft(Vector.empty[String])((acc, n) => acc :+ n)
    val methods = lines.map(_.split("\\s*=\\s*")).foldLeft(Vector.empty[Method]) { (acc, n) =>
      n match {
        case Array(alias, methodDesc) =>
          logger.info(s"smart contract exposes method [$alias = $methodDesc]")
          methodDesc.split("#") match {
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
                val method = types.Method(alias = alias,
                                          className = className,
                                          methodName = methodName,
                                          parameterTypes = parameterTypes)
                acc :+ method
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
      else Left(ContractCheckException(errors))
    } else Left(ContractCheckException(track.toVector))
  }
}
