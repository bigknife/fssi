package fssi
package sandbox
package world
import java.io._
import java.nio.file.{Files, Path, Paths}

import fssi.sandbox.contract.ContractFileBuilder
import fssi.sandbox.exception.{ContractBuildException, ContractCheckException}
import fssi.sandbox.inf._
import fssi.sandbox.types.{ContractMeta, SandBoxVersion}
import fssi.sandbox.visitor.clazz.DegradeClassVersionVisitor
import fssi.types.HexString
import fssi.types.base._
import fssi.types.biz.Contract.UserContract._
import fssi.types.biz.Contract.{Version => ContractVersion}
import fssi.types.biz.{Account, Contract}
import fssi.types.exception.FSSIException
import fssi.utils.FileUtil
import org.objectweb.asm.{ClassReader, ClassWriter}

import scala.collection.immutable.TreeSet

class Builder extends BaseLogger {

  private lazy val checker = new Checker

  private lazy val contractFileBuilder = new ContractFileBuilder

  def degradeClassVersion(rootPath: Path, targetPath: Path): Either[FSSIException, Unit] = {
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
            case Right(version) =>
              val classWriter = new ClassWriter(classReader, 0)
              val visitor     = DegradeClassVersionVisitor(classWriter, version)
              classReader.accept(visitor, 0)
              val array = classWriter.toByteArray
              outputStream.write(array, 0, array.length)
              outputStream.flush(); outputStream.close(); acc
            case Left(_) =>
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

  def buildUserContractFromPath(
      rootPath: Path,
      codeBytes: Array[Byte]): Either[FSSIException, Contract.UserContract] = {
    logger.info(s"build contract from path: $rootPath")
    if (rootPath.toFile.exists()) {
      for {
        contractMeta <- buildContractMeta(rootPath)
        _            <- checker.checkDeterminism(rootPath)
      } yield {
        import fssi.types.implicits._
        Contract.UserContract(
          owner = Account.ID(HexString.decode(contractMeta.owner.value).bytes),
          name = UniqueName(contractMeta.name.value),
          version = ContractVersion(contractMeta.version.value).get,
          code = Code(codeBytes),
          methods = TreeSet(contractMeta.interfaces.map(m => Method(m.alias, m.descriptor)): _*),
          signature = Signature.empty
        )
      }
    } else {
      val error =
        s"to build contract from file $rootPath not found: contract must be a file assembled all class files and contract descriptor"
      val ex = ContractBuildException(Vector(error))
      logger.error(error, ex)
      Left(ex)
    }
  }

  private[sandbox] def buildContractMeta(
      contractPath: Path): Either[FSSIException, ContractMeta] = {
    logger.info(s"build contract meta from contract file: $contractPath")
    import fssi.sandbox.types.Protocol._
    try {
      val contractDescriptorFile = Paths
        .get(contractPath.toString, s"META-INF/$metaFileName")
        .toFile
      if (contractDescriptorFile.exists && contractDescriptorFile.isFile) {
        checker.isContractMetaFileValid(contractDescriptorFile).map { _ =>
          val configReader      = ConfigReader(contractDescriptorFile)
          val methodDescriptors = configReader.methodDescriptors
          ContractMeta(owner = configReader.owner,
                       name = configReader.name,
                       version = configReader.version,
                       interfaces = methodDescriptors)
        }
      } else {
        val error =
          s"build user contract from file failed, can't not find contract meta conf in contract file $contractPath"
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
    }
  }

  def generateSandBoxContractFile(privateKeyBytes: Array[Byte],
                                  outputFile: File,
                                  contractBytes: Array[Byte]): Either[FSSIException, Unit] = {
    if (outputFile.exists()) FileUtil.deleteDir(outputFile.toPath)
    if (!outputFile.getParentFile.exists()) outputFile.getParentFile.mkdirs()
    outputFile.createNewFile()
    for {
      _         <- contractFileBuilder.addContractMagic(outputFile)
      _         <- contractFileBuilder.addContractSize(contractBytes.length.toLong, outputFile)
      _         <- contractFileBuilder.addSmartContract(contractBytes, outputFile)
      signature <- contractFileBuilder.makeContractSignature(privateKeyBytes, contractBytes)
      _         <- contractFileBuilder.addContractSignature(signature, outputFile)
    } yield ()
  }

  def readContractBytesFromFile(publicKey: Array[Byte],
                                file: File): Either[FSSIException, Array[Byte]] = {
    for {
      _             <- contractFileBuilder.readContractMagic(file)
      size          <- contractFileBuilder.readContractSize(file)
      contractBytes <- contractFileBuilder.readSmartContract(file, size)
      signature     <- contractFileBuilder.readContractSignature(file, size)
      _             <- contractFileBuilder.verifyContractSignature(publicKey, contractBytes, signature)
    } yield contractBytes
  }

  def buildContractProjectFromBytes(contractBytes: Array[Byte],
                                    rootPath: Path): Either[FSSIException, Path] = {
    val file = Paths.get(rootPath.toString, "contract-tmp").toFile
    try {
      val contractRootPath = Paths.get(rootPath.toString, "FSSIContract")
      better.files
        .File(file.toPath)
        .writeByteArray(contractBytes)
        .unzipTo(contractRootPath)(java.nio.charset.Charset.forName("utf-8"))
      Right(contractRootPath)
    } catch {
      case t: Throwable =>
        if (file.exists()) FileUtil.deleteDir(file.toPath)
        Left(
          new FSSIException(
            s"build contract root path with bytes under path: $rootPath failed: ${t.getMessage}"))
    } finally if (file.exists()) FileUtil.deleteDir(file.toPath)
  }
}
