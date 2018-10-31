package fssi
package sandbox
package world

import java.io._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import java.util.zip.{ZipEntry, ZipOutputStream}

import fssi.sandbox.exception.ContractCompileException
import fssi.sandbox.loader.FSSIClassLoader
import fssi.sandbox.types.SandBoxVersion
import fssi.sandbox.visitor.clazz.UpgradeClassVersionVisitor
import fssi.utils.FileUtil
import javax.tools.{DiagnosticCollector, JavaFileObject, ToolProvider}
import org.objectweb.asm.{ClassReader, ClassWriter}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import fssi.sandbox.inf._
import fssi.types.exception.FSSIException

class Compiler extends BaseLogger {

  private lazy val checker = new Checker
  private lazy val builder = new Builder

  /** compile contract
    *
    * @param rootPath project root path
    * @param outputFile path to store class file
    * @return errors if compiled failed
    */
  def compileContract(accountId: Array[Byte],
                      publicKeyBytes: Array[Byte],
                      privateKeyBytes: Array[Byte],
                      rootPath: Path,
                      sandBoxVersion: String,
                      outputFile: File): Either[FSSIException, Unit] = {
    logger.info(
      s"compile contract under path $rootPath at version $sandBoxVersion saved to $outputFile")
    if (rootPath.toFile.exists() && rootPath.toFile.isDirectory) {
      if (outputFile.exists()) FileUtil.deleteDir(outputFile.toPath)
      val out =
        Paths.get(outputFile.getParent, UUID.randomUUID().toString.replace("-", "")).toFile
      out.mkdirs()
      try {
        import fssi.sandbox.types.Protocol._
        for {
          _ <- checker.isProjectStructureValid(rootPath)
          resources     = Paths.get(rootPath.toString, "src/main/resources/META-INF").toFile
          resourceFiles = resources.listFiles().toVector
          _ <- checker.isResourceContractFilesInvalid(resources.toPath, resourceFiles)
          _ <- checker.isResourceFilesInValid(resources.toPath, resourceFiles)
          metaFile = Paths.get(resources.getAbsolutePath, metaFileName).toFile
          _ <- checker.isContractMetaFileValid(metaFile)
          configReader = ConfigReader(metaFile)
          methods <- checker.checkContractDescriptor(configReader.methodDescriptors)
          _       <- checker.isContractOwnerValid(accountId, configReader.owner)
          _       <- checker.isContractVersionValid(configReader.version)
          _       <- compileProject(rootPath, out.toPath)
          checkClassLoader = new FSSIClassLoader(out.toPath, ListBuffer.empty[String])
          _             <- checker.isContractMethodExisted(checkClassLoader, methods)
          boxVersion    <- SandBoxVersion(sandBoxVersion)
          _             <- checker.isSandBoxVersionValid(boxVersion)
          contractBytes <- upgradeAndZipContract(out.toPath, boxVersion)
          _             <- checker.isContractSizeValid(contractBytes.length.toLong)
          _             <- builder.generateSandBoxContractFile(privateKeyBytes, outputFile, contractBytes)
        } yield ()
      } catch {
        case t: Throwable =>
          if (outputFile.exists()) FileUtil.deleteDir(outputFile.toPath)
          val error =
            s"compile contract project under path $rootPath occurs errors: ${t.getMessage}"
          val ex = ContractCompileException(Vector(error))
          logger.error(error, t)
          Left(ex)
      } finally {
        if (out.exists()) FileUtil.deleteDir(out.toPath)
      }
    } else {
      val error = s"contract project $rootPath must be a directory"
      val ex    = ContractCompileException(Vector(error))
      logger.error(error, ex)
      Left(ex)
    }
  }

  /** compile contract project
    *
    * @param rootPath root path of project
    * @param outputPath compiled classes output path
    */
  private def compileProject(rootPath: Path, outputPath: Path): Either[FSSIException, Unit] = {
    logger.info(s"compile project $rootPath saved to $outputPath")
    import fssi.sandbox.types.Protocol._
    val javaFilePath = Paths.get(rootPath.toString, "src/main/java")
    val javaFiles    = FileUtil.findAllFiles(javaFilePath)
    val forbiddenFilesErrors = javaFiles.foldLeft(Vector.empty[String]) { (acc, f) =>
      val qualifiedClassName = f.getAbsolutePath.substring(javaFilePath.toString.length + 1)
      val existed =
        forbiddenPackage.exists(packageName => qualifiedClassName.startsWith(packageName))
      if (existed) acc :+ s"java source code file $qualifiedClassName is forbidden"
      else acc
    }
    if (forbiddenFilesErrors.isEmpty) {
      val javaCompiler    = ToolProvider.getSystemJavaCompiler
      val javaFileManager = javaCompiler.getStandardFileManager(null, null, null)
      val libPath         = Paths.get(rootPath.toString, "lib")
      val classPath = if (libPath.toFile.isDirectory) {
        libPath.toFile
          .list((dir: File, name: String) => name.endsWith(".jar"))
          .map(x => s"${libPath.toString}/$x")
          .toVector
          .mkString(":")
      } else ""
      val options             = Vector("-d", s"${outputPath.toString}", "-classpath", classPath)
      val diagnosticCollector = new DiagnosticCollector[JavaFileObject]
      val compilationTask = javaCompiler.getTask(
        null,
        null,
        diagnosticCollector,
        options.asJava,
        null,
        javaFileManager.getJavaFileObjects(
          javaFiles.map(_.getAbsolutePath).filter(_.endsWith(".java")): _*)
      )
      compilationTask.call() match {
        case _root_.java.lang.Boolean.TRUE =>
          object FV extends SimpleFileVisitor[Path] {
            override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
              val metaInf = file.toString.substring(
                Paths.get(rootPath.toString, "src/main/resources").toString.length + 1)
              val metaInfOut = Paths.get(outputPath.toString, metaInf)
              if (metaInfOut.toFile.isFile) metaInfOut.toFile.delete()

              Files.copy(file, metaInfOut)
              FileVisitResult.CONTINUE
            }
          }
          Paths.get(outputPath.toString, "META-INF").toFile.mkdirs()
          Files.walkFileTree(Paths.get(rootPath.toString, "src/main/resources"), FV)
          Right(())
        case _root_.java.lang.Boolean.FALSE =>
          val error = diagnosticCollector.getDiagnostics.asScala.toVector.map(_.getMessage(null))
          val ex    = ContractCompileException(error)
          logger.error(s"compile project occurs errors: $error", ex)
          Left(ex)
      }
    } else {
      val ex = ContractCompileException(forbiddenFilesErrors)
      logger.error(forbiddenFilesErrors.mkString(",\n"), ex)
      Left(ex)
    }
  }

  /** zip compiled contract
    * @param outPath compiled contract classes path
    * @param sandBoxVersion sand box version
    */
  private def upgradeAndZipContract(
      outPath: Path,
      sandBoxVersion: SandBoxVersion): Either[FSSIException, Array[Byte]] = {
    try {
      logger.info(s"upgrade contract class from version $sandBoxVersion ")
      val output     = new ByteArrayOutputStream()
      val zipOut     = new ZipOutputStream(output)
      val track      = scala.collection.mutable.ListBuffer.empty[String]
      val classFiles = FileUtil.findAllFiles(outPath).filter(_.getAbsolutePath.endsWith(".class"))
      classFiles.foreach { classFile =>
        if (classFile.canRead) {
          val fileInputStream = new FileInputStream(classFile)
          val cr              = new ClassReader(fileInputStream)
          val cw              = new ClassWriter(cr, 0)
          val visitor         = UpgradeClassVersionVisitor(cw, sandBoxVersion)
          cr.accept(visitor, 0)
          val classPath = classFile.getAbsolutePath
          val entryPath = classPath.substring(outPath.toString.length + 1)
          zipOut.putNextEntry(new ZipEntry(entryPath))
          zipOut.write(sandBoxVersion.toString.getBytes("utf-8"))
          val array = cw.toByteArray
          zipOut.write(array, 0, array.length)
          zipOut.closeEntry()
        } else
          track += s"class file ${classFile.getAbsolutePath} can not read"
      }
      val resourcesDir = Paths.get(outPath.toString, "META-INF").toFile
      val array        = new Array[Byte](8092)
      resourcesDir.listFiles().foreach { file =>
        val entryPath = file.getAbsolutePath.substring(outPath.toString.length + 1)
        zipOut.putNextEntry(new ZipEntry(entryPath))
        val inputStream = new FileInputStream(file)
        Iterator
          .continually(inputStream.read(array))
          .takeWhile(_ != -1)
          .foreach(read => zipOut.write(array, 0, read))
        zipOut.closeEntry()
        inputStream.close()
      }
      zipOut.flush(); output.flush(); zipOut.close(); output.close()
      if (track.isEmpty) Right(output.toByteArray)
      else {
        val ex = ContractCompileException(track.toVector)
        logger.error(
          s"upgrade contract class version and zip contract occurs errors: ${track.mkString(",\n")}",
          ex)
        Left(ex)
      }
    } catch {
      case t: Throwable =>
        val error = s"upgrade contract class file failed: ${t.getMessage}"
        val ex    = ContractCompileException(Vector(error))
        Left(ex)
    }
  }
}
