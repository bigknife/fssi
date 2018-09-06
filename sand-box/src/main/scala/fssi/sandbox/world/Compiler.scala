package fssi
package sandbox
package world

import java.io._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import java.util.zip.{ZipEntry, ZipOutputStream}

import fssi.sandbox.loader.FSSIClassLoader
import fssi.sandbox.visitor.UpgradeClassVersionVisitor
import fssi.sandbox.types.SandBoxVersion
import fssi.types.exception.ContractCompileException
import fssi.utils.FileUtil
import javax.tools.{DiagnosticCollector, JavaFileObject, ToolProvider}
import org.objectweb.asm.{ClassReader, ClassWriter}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class Compiler {

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
    if (rootPath.toFile.exists() && rootPath.toFile.isDirectory) {
      if (outputFile.exists() && outputFile.isFile) FileUtil.deleteDir(outputFile.toPath)
      val out =
        Paths.get(outputFile.getParent, UUID.randomUUID().toString.replace("-", "")).toFile
      out.mkdirs()
      try {
        import fssi.sandbox.types.Protocol._
        if (!checker.isProjectStructureValid(rootPath)) {
          Left(
            ContractCompileException(
              Vector("src path should have main/java and main/resources/META-INF subdirectories")))
        } else {
          val resources     = Paths.get(rootPath.toString, "src/main/resources/META-INF").toFile
          val resourceFiles = resources.listFiles().toVector
          val inValidContractFiles =
            checker.isResourceContractFilesInvalid(resources.toPath, resourceFiles)
          if (inValidContractFiles.nonEmpty) {
            Left(ContractCompileException(Vector(
              s"smart contract required files not found: ${inValidContractFiles.mkString(" , ")}")))
          } else {
            val inValidFiles = checker.isResourceFilesInValid(resources.toPath, resourceFiles)
            if (inValidFiles.nonEmpty) {
              Left(
                ContractCompileException(
                  Vector(s"src/main/resources/META-INF dir only support ${allowedResourceFiles
                    .mkString("、")} files,but found ${inValidFiles.mkString(" , ")}")))
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
                              if (outputFile.length() > contractSize) {
                                FileUtil.deleteDir(outputFile.toPath)
                                Left(ContractCompileException(Vector(
                                  s"contract project total size can not exceed $contractSize bytes")))
                              } else Right(())
                          }
                        case None =>
                          Left(ContractCompileException(Vector(
                            s"java version $version not support,correct version must between 6 and 10")))
                      }
                    }
                }
            }
          }
        }
      } catch {
        case t: Throwable => Left(ContractCompileException(Vector(t.getMessage)))
      } finally {
        if (out.exists()) FileUtil.deleteDir(out.toPath)
      }
    } else Left(ContractCompileException(Vector("contract project must be a directory")))
  }

  /** compile contract project
    *
    * @param rootPath root path of project
    * @param outputPath compiled classes output path
    */
  private[world] def compileProject(rootPath: Path,
                                    outputPath: Path): Either[ContractCompileException, Unit] = {
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
      var classPath       = System.getProperty("java.class.path")
      if (libPath.toFile.isDirectory) {
        classPath = libPath.toFile
          .list((dir: File, name: String) => name.endsWith(".jar"))
          .foldLeft(classPath) { (acc, n) =>
            acc + ":" + Paths.get(libPath.toString, n)
          }
      }
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
          Left(
            ContractCompileException(
              diagnosticCollector.getDiagnostics.asScala.toVector.map(_.getMessage(null))))
      }
    } else Left(ContractCompileException(forbiddenFilesErrors))
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
    if (outputFile.exists() && outputFile.isDirectory)
      Left(ContractCompileException(Vector("compiled contract output file can not be a directory")))
    else {
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
        Left(ContractCompileException(track.toVector))
      }
    }
  }
}
