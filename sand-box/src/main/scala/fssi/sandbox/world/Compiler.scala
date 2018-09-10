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
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class Compiler {

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  private lazy val checker = new Checker

  /** compile contract
    *
    * @param rootPath project root path
    * @param outputFile path to store class file
    * @return errors if compiled failed
    */
  def compileContract(rootPath: Path,
                      version: String,
                      outputFile: File): Either[ContractCompileException, Unit] = {
    logger.info(s"compile contract under path $rootPath at version $version saved to $outputFile")
    if (rootPath.toFile.exists() && rootPath.toFile.isDirectory) {
      if (outputFile.exists() && outputFile.isFile) FileUtil.deleteDir(outputFile.toPath)
      val out =
        Paths.get(outputFile.getParent, UUID.randomUUID().toString.replace("-", "")).toFile
      out.mkdirs()
      try {
        import fssi.sandbox.types.Protocol._
        if (!checker.isProjectStructureValid(rootPath)) {
          val error =
            s"$rootPath src path should have main/java and main/resources/META-INF subdirectories"
          val ex = ContractCompileException(Vector(error))
          logger.error(error, ex)
          Left(ex)
        } else {
          val resources     = Paths.get(rootPath.toString, "src/main/resources/META-INF").toFile
          val resourceFiles = resources.listFiles().toVector
          val inValidContractFiles =
            checker.isResourceContractFilesInvalid(resources.toPath, resourceFiles)
          if (inValidContractFiles.nonEmpty) {
            val error =
              s"smart contract required files not found: ${inValidContractFiles.mkString(" , ")}"
            val ex = ContractCompileException(Vector(error))
            logger.error(error, ex)
            Left(ex)
          } else {
            val inValidFiles = checker.isResourceFilesInValid(resources.toPath, resourceFiles)
            if (inValidFiles.nonEmpty) {
              val error = s"src/main/resources/META-INF dir only support ${allowedResourceFiles
                .mkString("、")} files,but found ${inValidFiles.mkString(" , ")}"
              val ex = ContractCompileException(Vector(error))
              logger.error(error, ex)
              Left(ex)
            } else {
              checker
                .checkContractDescriptor(
                  Paths.get(resources.getAbsolutePath, contractFileName).toFile)
                .left
                .map(x => ContractCompileException(x.messages))
                .right
                .flatMap { _ =>
                  val compileEither = compileProject(rootPath, out.toPath)
                  compileEither
                    .flatMap { _ =>
                      val track            = ListBuffer.empty[String]
                      val targetPath       = out.toPath
                      val checkClassLoader = new FSSIClassLoader(targetPath, track)
                      checker
                        .checkContractMethod(targetPath, track, checkClassLoader)
                        .left
                        .map(x => ContractCompileException(x.messages))
                    }
                    .flatMap { _ =>
                      SandBoxVersion(version) match {
                        case Some(sandBoxVersion) =>
                          upgradeAndZipContract(out.toPath, sandBoxVersion, outputFile).flatMap {
                            _ =>
                              import fssi.sandbox.types.Protocol._
                              val outputFileSize = outputFile.length()
                              if (outputFileSize > contractSize) {
                                FileUtil.deleteDir(outputFile.toPath)
                                val error =
                                  s"contract project compiled file total size $outputFileSize can not exceed $contractSize bytes"
                                val ex = ContractCompileException(Vector(error))
                                logger.error(error, ex)
                                Left(ex)
                              } else Right(())
                          }
                        case None =>
                          val error =
                            s"java version $version not support,correct version must between 6 and 10"
                          val ex = ContractCompileException(Vector(error))
                          logger.error(error, ex)
                          Left(ex)
                      }
                    }
                }
            }
          }
        }
      } catch {
        case t: Throwable =>
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
  private[world] def compileProject(rootPath: Path,
                                    outputPath: Path): Either[ContractCompileException, Unit] = {
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
//      var classPath       = System.getProperty("java.class.path")
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
    *
    * @param outPath compiled contract classes path
    * @param sandBoxVersion sand box version
    * @param outputFile file to store compiled contract
    */
  private[world] def upgradeAndZipContract(
      outPath: Path,
      sandBoxVersion: SandBoxVersion,
      outputFile: File): Either[ContractCompileException, Unit] = {
    logger.info(
      s"upgrade contract class to version ${sandBoxVersion.value} and zip to file $outputFile")
    if (outputFile.exists() && outputFile.isDirectory) {
      val error = s"compiled contract output file $outputFile can not be a directory"
      val ex    = ContractCompileException(Vector(error))
      logger.error(error, ex)
      Left(ex)
    } else {
      if (!outputFile.exists()) outputFile.createNewFile()
      val output     = new FileOutputStream(outputFile, true)
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
      if (track.isEmpty) Right(())
      else {
        if (outputFile.exists() && outputFile.isFile) outputFile.delete()
        val ex = ContractCompileException(track.toVector)
        logger.error(
          s"upgrade contract class version and zip contract occurs errors: ${track.mkString(",\n")}",
          ex)
        Left(ex)
      }
    }
  }
}
