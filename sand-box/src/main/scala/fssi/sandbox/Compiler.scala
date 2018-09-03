package fssi
package sandbox

import java.io.{BufferedReader, File, FileReader}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import fssi.types.Contract.ParameterType
import fssi.types.exception.{ContractCheckException, ContractCompileException}
import fssi.utils.FileUtil
import javax.tools.{DiagnosticCollector, JavaFileObject, ToolProvider}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

trait Compiler {

  lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  /** compile contract
    * @param rootPath project root path
    * @param outputFile path to store class file
    * @return errors if compiled failed
    */
  def compileContract(rootPath: Path,
                      version: String,
                      outputFile: File): Either[ContractCompileException, Unit] = {
    // project dir should exist src/main/java and /src/main/resources subdirectories
    if (!Paths.get(rootPath.toString, "main/java").toFile.isDirectory ||
        !Paths.get(rootPath.toString, "main/resources").toFile.isDirectory) {
      Left(
        ContractCompileException(
          Vector("src path should have src/main/java and /src/main/resources subdirectories")))
    } else {
      val javaCompiler    = ToolProvider.getSystemJavaCompiler
      val javaFileManager = javaCompiler.getStandardFileManager(null, null, null)
      val libPath         = Paths.get(rootPath.getParent.toString, "lib")
      var classPath       = System.getProperty("java.class.path")
      if (libPath.toFile.isDirectory) {
        classPath = libPath.toFile
          .list((dir: File, name: String) => name.endsWith(".jar"))
          .foldLeft(classPath) { (acc, n) =>
            acc + ":" + Paths.get(libPath.toString, n)
          }
      }
      val options             = Vector("-d", s"${outputFile.getAbsolutePath}", "-classpath", classPath)
      val diagnosticCollector = new DiagnosticCollector[JavaFileObject]
      val compilationTask = javaCompiler.getTask(
        null,
        null,
        diagnosticCollector,
        options.asJava,
        null,
        javaFileManager.getJavaFileObjects(findAllJavaFiles(rootPath): _*))
      compilationTask.call() match {
        case _root_.java.lang.Boolean.TRUE =>
          object FV extends SimpleFileVisitor[Path] {
            override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
              val metaInf = file.toString.substring(
                Paths.get(rootPath.toString, "main/resources").toString.length)
              val metaInfOut = Paths.get(outputFile.toString, metaInf)
              if (metaInfOut.toFile.isFile) metaInfOut.toFile.delete()

              Files.copy(file, metaInfOut)
              FileVisitResult.CONTINUE
            }
          }
          Paths.get(outputFile.toString, "META-INF").toFile.mkdirs()
          Files.walkFileTree(Paths.get(rootPath.toString, "main/resources"), FV)
          Right(())
        case _root_.java.lang.Boolean.FALSE =>
          Left(
            ContractCompileException(
              diagnosticCollector.getDiagnostics.asScala.toVector.map(_.getMessage(null))))
      }
    }
  }

  private def findAllJavaFiles(src: Path): Vector[String] = {
    def findJavaFileByDir(dir: File, accFiles: Vector[String]): Vector[String] = dir match {
      case f if f.isFile && f.getAbsolutePath.endsWith(".java") => accFiles :+ dir.getAbsolutePath
      case f if f.isFile                                        => accFiles
      case d                                                    => d.listFiles.toVector.foldLeft(accFiles)((f, v) => findJavaFileByDir(v, f))
    }
    findJavaFileByDir(src.toFile, Vector.empty)
  }

  /** check contract class file determinism
    * @param rootPath class file root path
    * @return errors when check failed
    */
  def checkDeterminism(rootPath: Path): Either[ContractCheckException, Unit] = {
    val contract = Paths.get(rootPath.toString, "META-INF/contract").toFile
    if (!contract.exists() || !contract.isFile)
      Left(ContractCheckException(Vector("META-INF/contract file not found")))
    else {
      val reader = new BufferedReader(new FileReader(contract))
      val lines = Iterator
        .continually(reader.readLine())
        .takeWhile(_ != null)
        .foldLeft(Vector.empty[String])((acc, n) => acc :+ n)
      val track            = scala.collection.mutable.ListBuffer.empty[String]
      val checkClassLoader = new FSSIClassLoader(rootPath, track)
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
                .map(ParameterType(_))
              checkClassLoader.findClassMethod(clazz, methodName, parameterTypes.map(_.`type`))
            }
        }
      }
      if (track.isEmpty) Right(())
      else { FileUtil.deleteDir(rootPath); Left(ContractCheckException(track.toVector)) }
    }
  }
}

object Compiler extends Compiler
