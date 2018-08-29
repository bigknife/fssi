package fssi.utils

import java.io.{BufferedReader, File, FileReader}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import javax.tools.{DiagnosticCollector, JavaFileObject, ToolProvider}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

/**
  * Created on 2018/8/14
  */
trait Compiler {

  lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  /***
    * compile source code
    * @param sourceDir project path
    * @param targetDir path to store class file
    * @return class file path or errors
    */
  def compileToNormalClass(sourceDir: Path, targetDir: Path): Either[Vector[String], Path] = {
    // project dir should exist src/main/java and /src/main/resources subdirectories
    if (!Paths.get(sourceDir.toString, "main/java").toFile.isDirectory ||
        !Paths.get(sourceDir.toString, "main/resources").toFile.isDirectory) {
      Left(Vector("src path should have src/main/java and /src/main/resources subdirectories"))
    } else {
      val javaCompiler    = ToolProvider.getSystemJavaCompiler
      val javaFileManager = javaCompiler.getStandardFileManager(null, null, null)
      val libPath         = Paths.get(sourceDir.getParent.toString, "lib")
      var classPath       = System.getProperty("java.class.path")
      if (libPath.toFile.isDirectory) {
        classPath = libPath.toFile
          .list((dir: File, name: String) ⇒ name.endsWith(".jar"))
          .foldLeft(classPath) { (acc, n) ⇒
            acc + ":" + Paths.get(libPath.toString, n)
          }
      }
      val options             = Vector("-d", s"${targetDir.toFile.getAbsolutePath}", "-classpath", classPath)
      val diagnosticCollector = new DiagnosticCollector[JavaFileObject]
      val compilationTask = javaCompiler.getTask(
        null,
        null,
        diagnosticCollector,
        options.asJava,
        null,
        javaFileManager.getJavaFileObjects(findAllJavaFiles(sourceDir): _*))
      compilationTask.call() match {
        case _root_.java.lang.Boolean.TRUE ⇒
          object FV extends SimpleFileVisitor[Path] {
            override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
              val metaInf = file.toString.substring(
                Paths.get(sourceDir.toString, "main/resources").toString.length)
              val metaInfOut = Paths.get(targetDir.toString, metaInf)
              if (metaInfOut.toFile.isFile) metaInfOut.toFile.delete()

              Files.copy(file, metaInfOut)
              FileVisitResult.CONTINUE
            }
          }
          Paths.get(targetDir.toString, "META-INF").toFile.mkdirs()
          Files.walkFileTree(Paths.get(sourceDir.toString, "main/resources"), FV)
          Right(targetDir)
        case _root_.java.lang.Boolean.FALSE ⇒
          Left(diagnosticCollector.getDiagnostics.asScala.toVector.map(_.getMessage(null)))
      }
    }
  }

  private def findAllJavaFiles(src: Path): Vector[String] = {
    def findJavaFileByDir(dir: File, accFiles: Vector[String]): Vector[String] = dir match {
      case f if f.isFile && f.getAbsolutePath.endsWith(".java") ⇒ accFiles :+ dir.getAbsolutePath
      case f if f.isFile                                        ⇒ accFiles
      case d                                                    ⇒ d.listFiles.toVector.foldLeft(accFiles)((f, v) ⇒ findJavaFileByDir(v, f))
    }
    findJavaFileByDir(src.toFile, Vector.empty)
  }

  /**
    * check class file determinism
    * @param classFilePath class file root path
    * @return errors when check failed
    */
  def checkDeterminism(classFilePath: Path): Either[Vector[String], Unit] = {
    val contract = Paths.get(classFilePath.toString, "META-INF/contract").toFile
    if (!contract.exists() || !contract.isFile) Left(Vector("META-INF/contract file not found"))
    else {
      val reader = new BufferedReader(new FileReader(contract))
      val lines = Iterator
        .continually(reader.readLine())
        .takeWhile(_ != null)
        .foldLeft(Vector.empty[String])((acc, n) ⇒ acc :+ n)
      val track            = CheckingClassLoader.ClassCheckingStatus()
      val checkClassLoader = new CheckingClassLoader(classFilePath, track)
      lines.map(_.split("\\s*=\\s*")(1).trim).foreach { x ⇒
        if (logger.isInfoEnabled()) logger.info(s"smart contract exposes class $x")
        x.split("#") match {
          case Array(c, m) ⇒ checkClassLoader.findClass(c)
          case Array(c)    ⇒ checkClassLoader.findClass(c)
        }
      }
      if (track.isLegal) Right(())
      else { FileUtil.deleteDir(classFilePath); Left(track.errors) }
    }
  }
}

object Compiler extends Compiler
