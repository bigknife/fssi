package fssi
package sandbox

import java.io._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import java.util.zip.{ZipEntry, ZipOutputStream}

import fssi.sandbox.visitor.{DegradeClassVersionVisitor, UpgradeClassVersionVisitor}
import fssi.types.exception.{ContractCheckException, ContractCompileException}
import fssi.utils.FileUtil
import javax.tools.{DiagnosticCollector, JavaFileObject, ToolProvider}
import org.objectweb.asm.{ClassReader, ClassWriter}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class Compiler {

  lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  /** compile contract
    *
    * @param rootPath project root path
    * @param outputFile path to store class file
    * @return errors if compiled failed
    */
  def compileContract(rootPath: Path,
                      version: String,
                      outputFile: File): Either[ContractCompileException, Unit] = {
    import Protocol._
    if (!isProjectStructureValid(rootPath)) {
      Left(
        ContractCompileException(
          Vector("src path should have main/java and main/resources/META-INF subdirectories")))
    } else {
      val resources            = Paths.get(rootPath.toString, "src/main/resources/META-INF").toFile
      val resourceFiles        = resources.listFiles().toVector
      val inValidContractFiles = isResourceContractFilesInvalid(resources.toPath, resourceFiles)
      if (inValidContractFiles.nonEmpty) {
        Left(
          ContractCompileException(
            Vector(
              s"smart contract required files not found: ${inValidContractFiles.mkString(" , ")}")))
      } else {
        val inValidFiles = isResourceFilesInValid(resources.toPath, resourceFiles)
        if (inValidFiles.nonEmpty) {
          Left(
            ContractCompileException(
              Vector(s"src/main/resources/META-INF dir only support ${allowedResourceFiles.mkString(
                "、")} files,but found ${inValidFiles.mkString(" , ")}")))
        } else {
          val out =
            Paths.get(outputFile.getParent, UUID.randomUUID().toString.replace("-", "")).toFile
          out.mkdirs()
          val compileEither = compileProject(rootPath, out.toPath)
          val result = compileEither.flatMap { _ =>
            SandBoxVersion(version) match {
              case Some(sandBoxVersion) =>
                upgradeAndZipContract(out.toPath, sandBoxVersion, outputFile)
              case None =>
                Left(
                  ContractCompileException(Vector(
                    s"java version $version not support,correct version must between 6 and 10")))
            }
          }
          if (out.exists()) FileUtil.deleteDir(out.toPath)
          result
        }
      }
    }
  }

  /** check contract class file determinism
    *
    * @param rootPath class file root path
    * @return errors when check failed
    */
  def checkDeterminism(rootPath: Path): Either[ContractCheckException, Unit] = {
    val track      = scala.collection.mutable.ListBuffer.empty[String]
    val targetPath = Paths.get(rootPath.getParent.toString, "fssi")
    if (!targetPath.toFile.exists()) targetPath.toFile.mkdirs()
    for {
      _ <- degradeClassVersion(rootPath, targetPath)
      checkClassLoader = new FSSIClassLoader(targetPath, track)
      _ <- checkContractMethod(targetPath, track, checkClassLoader)
      _ <- checkClasses(targetPath, track, checkClassLoader)
    } yield FileUtil.deleteDir(targetPath)
  }

  def degradeClassVersion(rootPath: Path,
                          targetPath: Path): Either[ContractCheckException, Unit] = {
    val metaInfoPath = Paths.get(targetPath.toString, "META-INF")
    if (metaInfoPath.toFile.exists()) metaInfoPath.toFile.delete()
    metaInfoPath.toFile.mkdirs()
    val rootResourcesPath = Paths.get(rootPath.toString, "META-INF")
    val resourcesFiles    = findAllFiles(rootResourcesPath)
    resourcesFiles.foreach { resourceFile =>
      val path =
        Paths.get(metaInfoPath.toString,
                  resourceFile.getAbsolutePath.substring(rootResourcesPath.toString.length + 1))
      if (path.toFile.exists() && path.toFile.isFile) path.toFile.delete()
      Files.copy(resourceFile.toPath, path)
    }
    val classFiles = findAllFiles(rootPath).filter(_.getAbsolutePath.endsWith(".class"))
    val buffer     = new Array[Byte](8092)
    val degradeErrors = classFiles.foldLeft(Vector.empty[String]) { (acc, classFile) =>
      if (classFile.canRead) {
        val filePath = Paths.get(targetPath.toString,
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
  }

  private def checkClasses(
      rootPath: Path,
      track: ListBuffer[String],
      checkClassLoader: FSSIClassLoader): Either[ContractCheckException, Unit] = {
    val classFiles = findAllFiles(rootPath).filter(_.getAbsolutePath.endsWith(".class"))
    classFiles.foreach { file =>
      val classFileName =
        file.getAbsolutePath.substring(rootPath.toString.length + 1).replace("/", ".")
      val className = classFileName.substring(0, classFileName.lastIndexOf(".class"))
      checkClassLoader.findClassFromClassFile(file, className, "", Array.empty)
    }
    if (track.isEmpty) Right(())
    else Left(ContractCheckException(track.toVector))
  }

  private def checkContractMethod(
      rootPath: Path,
      track: ListBuffer[String],
      checkClassLoader: FSSIClassLoader): Either[ContractCheckException, Unit] = {
    val contract = Paths.get(rootPath.toString, "META-INF/contract").toFile
    if (!contract.exists() || !contract.isFile)
      Left(ContractCheckException(Vector("META-INF/contract file not found")))
    else {
      val reader = new BufferedReader(new FileReader(contract))
      val lines = Iterator
        .continually(reader.readLine())
        .takeWhile(_ != null)
        .foldLeft(Vector.empty[String])((acc, n) => acc :+ n)
      lines.map(_.split("\\s*=\\s*")(1).trim).foreach { x =>
        if (logger.isInfoEnabled()) logger.info(s"smart contract exposes class method [$x]")
        x.split("#") match {
          case Array(clazz, method) =>
            val leftIndex  = method.indexOf("(")
            val rightIndex = method.lastIndexOf(")")
            if (leftIndex < 0 || rightIndex < 0) track += s"contract descriptor invalid: $x"
            else {
              val methodName = method.substring(0, leftIndex)
              val parameterTypes = method
                .substring(leftIndex + 1, rightIndex)
                .split(",")
                .filter(_.nonEmpty)
                .map(SParameterType(_))
              checkClassLoader.findClass(clazz, methodName, parameterTypes.map(_.`type`))
            }
        }
      }
      if (track.isEmpty) Right(())
      else {
        FileUtil.deleteDir(rootPath); Left(ContractCheckException(track.toVector))
      }
    }
  }

  private def findAllFiles(src: Path): Vector[File] = {
    def findFileByDir(dir: File, accFiles: Vector[File]): Vector[File] = dir match {
      case f if f.isFile => accFiles :+ f
      case d             => d.listFiles.toVector.foldLeft(accFiles)((f, v) => findFileByDir(v, f))
    }
    findFileByDir(src.toFile, Vector.empty)
  }

  /** compile contract project
    *
    * @param rootPath root path of project
    * @param outputPath compiled classes output path
    */
  private def compileProject(rootPath: Path,
                             outputPath: Path): Either[ContractCompileException, Unit] = {
    import Protocol._
    val javaFilePath = Paths.get(rootPath.toString, "src/main/java")
    val javaFiles    = findAllFiles(javaFilePath)
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
  private def upgradeAndZipContract(outPath: Path,
                                    sandBoxVersion: SandBoxVersion,
                                    outputFile: File): Either[ContractCompileException, Unit] = {
    if (outputFile.exists() && outputFile.isDirectory)
      Left(ContractCompileException(Vector("compiled contract output file can not be a directory")))
    else {
      if (!outputFile.exists()) outputFile.createNewFile()
      val output     = new FileOutputStream(outputFile, true)
      val zipOut     = new ZipOutputStream(output)
      val track      = scala.collection.mutable.ListBuffer.empty[String]
      val classFiles = findAllFiles(outPath).filter(_.getAbsolutePath.endsWith(".class"))
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
      FileUtil.deleteDir(outPath)
      if (track.isEmpty) Right(())
      else {
        if (outputFile.exists() && outputFile.isFile) outputFile.delete()
        Left(ContractCompileException(track.toVector))
      }
    }
  }

  private def isProjectStructureValid(rootPath: Path): Boolean = {
    Paths.get(rootPath.toString, "src/main/java").toFile.isDirectory &&
    Paths.get(rootPath.toString, "src/main/resources/META-INF").toFile.isDirectory
  }

  private def isResourceFilesInValid(resourcesRoot: Path,
                                     resourceFiles: Vector[File]): Vector[String] = {
    import Protocol._
    resourceFiles
      .map(_.getAbsolutePath)
      .foldLeft(Vector.empty[String]) { (acc, n) =>
        val fileName = n.substring(resourcesRoot.toString.length + 1)
        if (!allowedResourceFiles.contains(fileName)) acc :+ fileName
        else acc
      }
  }

  private def isResourceContractFilesInvalid(resourcesRoot: Path,
                                             resourceFiles: Vector[File]): Vector[String] = {
    import Protocol._
    allowedResourceFiles.foldLeft(Vector.empty[String]) { (acc, n) =>
      val existed = resourceFiles.map(_.getAbsolutePath).exists { filePath =>
        val contractFileName = filePath.substring(resourcesRoot.toString.length + 1)
        n == contractFileName
      }
      if (existed) acc
      else acc :+ n
    }
  }
}
