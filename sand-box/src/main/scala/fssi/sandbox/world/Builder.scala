package fssi
package sandbox
package world
import java.io.{ByteArrayOutputStream, File, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Path, Paths}

import fssi.sandbox.exception.{ContractBuildException, ContractCheckException}
import fssi.sandbox.visitor.DegradeClassVersionVisitor
import fssi.types.Contract.{Meta, Method}
import fssi.types._
import fssi.utils.FileUtil
import org.objectweb.asm.{ClassReader, ClassWriter}

import scala.collection.immutable.TreeSet

class Builder {

  private lazy val checker = new Checker

  def degradeClassVersion(rootPath: Path,
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

  def buildUserContractFromFile(
      accountId: Account.ID,
      file: File,
      name: UniqueName,
      version: Version): Either[ContractBuildException, Contract.UserContract] = {
    if (file.exists() && file.isFile) {
      for {
        methods <- buildContractMethod(file)
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
          owner = accountId,
          name = name,
          version = version,
          code = Base64String(byteArrayOutputStream.toByteArray),
          meta = Meta(methods = TreeSet(methods.map(m => Method(m.alias)): _*)),
          signature = Signature.empty
        )
      }
    } else
      Left(
        ContractBuildException(
          Vector("contract must be a file assembled all class files and contract descriptor")))
  }

  private[sandbox] def buildContractMethod(
      contractFile: File): Either[ContractBuildException, Vector[fssi.sandbox.types.Method]] = {
    import fssi.sandbox.types.Protocol._
    val cache = Paths.get(contractFile.getParent, "cache")
    if (cache.toFile.exists()) FileUtil.deleteDir(cache)
    cache.toFile.mkdirs()
    try {
      val unzipDir = better.files.File(contractFile.toPath).unzipTo(cache)
      val contractDescriptorFile = Paths
        .get(unzipDir.pathAsString, s"META-INF/$contractFileName")
        .toFile
      for {
        methods <- checker
          .checkContractDescriptor(contractDescriptorFile)
          .left
          .map(x => ContractBuildException(x.messages))
      } yield methods
    } catch {
      case t: Throwable => Left(ContractBuildException(Vector(t.getMessage)))
    } finally { if (cache.toFile.exists()) FileUtil.deleteDir(cache) }
  }
}
