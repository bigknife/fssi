package fssi
package sandbox
package world
import java.io.{ByteArrayOutputStream, File, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Path, Paths}

import fssi.sandbox.exception.{ContractBuildException, ContractCheckException}
import fssi.sandbox.types.SandBoxVersion
import fssi.sandbox.visitor.clazz.DegradeClassVersionVisitor
import fssi.types.Contract.{Meta, Method}
import fssi.types.{Version => ContractVersion, _}
import fssi.utils.FileUtil
import org.objectweb.asm.{ClassReader, ClassWriter}
import org.slf4j.{Logger, LoggerFactory}
import fssi.sandbox.config._
import fssi.sandbox.types.ContractMeta
import fssi.sandbox.config._
import scala.collection.immutable.TreeSet

class Builder {

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  private lazy val checker = new Checker

  def degradeClassVersion(rootPath: Path,
                          targetPath: Path): Either[ContractCheckException, Unit] = {
    logger.info(s"degrade class version for dir: $rootPath saved to $targetPath")
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
                                                          Integer.valueOf(5),
                                                          java.lang.Boolean.valueOf(false))
          readerConstructor.setAccessible(accessible)
          val versionStr = new String(classBuffer, 0, 5, java.nio.charset.Charset.forName("utf-8"))
          SandBoxVersion(versionStr) match {
            case Some(version) =>
              val classWriter = new ClassWriter(classReader, 0)
              val visitor     = DegradeClassVersionVisitor(classWriter, version)
              classReader.accept(visitor, 0)
              val array = classWriter.toByteArray
              outputStream.write(array, 0, array.length)
              outputStream.flush(); outputStream.close(); acc
            case None =>
              acc :+ s"degrade class version failed: fssi contract class file first 5 bytes must be sandbox version,but found $versionStr"
          }
        } else acc :+ s"class file ${classFile.getAbsolutePath} can not read"
      }
      if (degradeErrors.isEmpty) Right(())
      else {
        val ex = ContractCheckException(degradeErrors)
        logger.error(ex.getMessage, ex)
        Left(ex)
      }
    } catch {
      case t: Throwable =>
        val error = s"degrade class version occurs error: ${t.getMessage}"
        logger.error(error, t)
        Left(ContractCheckException(Vector(error)))
    }
  }

  def buildUserContractFromFile(
      file: File): Either[ContractBuildException, Contract.UserContract] = {
    logger.info(s"build contract from file: $file")
    if (file.exists() && file.isFile) {
      for {
        contractMeta <- buildContractMeta(file)
        _ <- checker
          .checkDeterminism(file)
          .right
          .map(_ => Vector.empty[Method])
          .left
          .map(x => ContractBuildException(x.messages))
      } yield {
        val fileInputStream       = new FileInputStream(file)
        val byteArrayOutputStream = new ByteArrayOutputStream
        val array                 = new Array[Byte](8092)
        Iterator
          .continually(fileInputStream.read(array))
          .takeWhile(_ != -1)
          .foreach(read => byteArrayOutputStream.write(array, 0, read))
        byteArrayOutputStream.flush(); fileInputStream.close()
        import implicits._
        Contract.UserContract(
          owner = Account.ID(HexString.decode(contractMeta.owner.value)),
          name = UniqueName(contractMeta.name.value),
          version = ContractVersion(contractMeta.version.value),
          code = Base64String(byteArrayOutputStream.toByteArray),
          meta = Meta(methods = TreeSet(contractMeta.interfaces.map(m => Method(m.alias)): _*)),
          signature = Signature.empty
        )
      }
    } else {
      val error =
        s"to build contract from file $file not found: contract must be a file assembled all class files and contract descriptor"
      val ex = ContractBuildException(Vector(error))
      logger.error(error, ex)
      Left(ex)
    }
  }

  private[sandbox] def buildContractMeta(
      contractFile: File): Either[ContractBuildException, ContractMeta] = {
    logger.info(s"build contract meta from contract file: $contractFile")
    import fssi.sandbox.types.Protocol._
    val cache = Paths.get(contractFile.getParent, "cache")
    try {
      if (cache.toFile.exists()) FileUtil.deleteDir(cache)
      cache.toFile.mkdirs()
      val unzipDir = better.files
        .File(contractFile.toPath)
        .unzipTo(cache)(java.nio.charset.Charset.forName("utf-8"))
      val contractDescriptorFile = Paths
        .get(unzipDir.pathAsString, s"META-INF/$metaFileName")
        .toFile
      if (contractDescriptorFile.exists && contractDescriptorFile.isFile) {
        checker
          .isContractMetaFileValid(contractDescriptorFile)
          .left
          .map(x => ContractBuildException(x.messages))
          .map { _ =>
            val configReader      = ConfigReader(contractDescriptorFile)
            val methodDescriptors = configReader.interfaces
            ContractMeta(owner = configReader.owner,
                         name = configReader.name,
                         version = configReader.version,
                         interfaces = methodDescriptors)
          }
      } else {
        val error =
          s"build user contract from file failed, can't not find contract meta conf in contract file $contractFile"
        val ex = ContractBuildException(Vector(error))
        logger.error(error, ex)
        Left(ex)
      }
    } catch {
      case t: Throwable =>
        val error = s"build user contract from file failed: ${t.getMessage}"
        val ex    = ContractBuildException(Vector(error))
        logger.error(error, ex)
        Left(ex)
    } finally if (cache.toFile.exists()) FileUtil.deleteDir(cache)
  }
}
