package fssi.sandbox

import _root_.java.io.{BufferedReader, File, FileReader, FilenameFilter}
import _root_.java.nio.file._
import _root_.java.nio.file.attribute.BasicFileAttributes

import javax.tools._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

trait Compiler {
  private val logger = LoggerFactory.getLogger(getClass)
  /** compile src to normal class files, no determinism checking.
    *
    * @param src source code path
    * @param out class files path
    * @return
    */
  def compileToNormalClasses(src: Path, out: Path): Either[Vector[String], Path] = {
    // check the src path scheme
    //  1. should have a sub dir src/main/java
    //  2. should have a sub dir src/main/resources
    if (!Paths.get(src.toString, "main/java").toFile.isDirectory ||
        !Paths.get(src.toString, "main/resources/META-INF").toFile.isDirectory) {
      Left(Vector("src path should has two sub directory: main/java and main/resources/META-INF"))
    } else {

      val javaCompiler                  = ToolProvider.getSystemJavaCompiler
      val sjfm: StandardJavaFileManager = javaCompiler.getStandardFileManager(null, null, null)

      val libPath = Paths.get(src.getParent.toString, "lib")

      var classPath = System.getProperty("java.class.path")
      if(libPath.toFile.isDirectory) {
        classPath = libPath.toFile.list(new FilenameFilter {
          override def accept(dir: File, name: String): Boolean = name.endsWith(".jar")
        }).foldLeft(classPath) {(acc, n) =>
          classPath + ":" + Paths.get(libPath.toString, n)
        }
      }

      val options = Vector("-d", s"${out.toFile.getAbsolutePath}",
      "-classpath", classPath)

      val diagnosticCollector = new DiagnosticCollector[JavaFileObject]()
      val compilationTask = javaCompiler.getTask(
        null,
        null,
        diagnosticCollector,
        options.toIterable.asJava,
        null,
        sjfm.getJavaFileObjects(allFiles(src, ".java").toList: _*))

      compilationTask.call() match {
        case _root_.java.lang.Boolean.TRUE =>
          // after compilation done, copy the META-INF to the out
          object FV extends SimpleFileVisitor[Path] {
            override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
              val meta_inf = file.toString.substring(Paths.get(src.toString, "main/resources").toString.length)
              val meta_inf_out = Paths.get(out.toString, meta_inf)

              if(meta_inf_out.toFile.isFile) meta_inf_out.toFile.delete()

              Files.copy(file, meta_inf_out)
              FileVisitResult.CONTINUE
            }
          }
          Paths.get(out.toString, "META-INF").toFile.mkdirs()
          Files.walkFileTree(Paths.get(src.toString, "main/resources"), FV)
          Right(out)
        case _root_.java.lang.Boolean.FALSE =>
          Left(diagnosticCollector.getDiagnostics.asScala.toVector.map { x =>
            x.getMessage(null)
          })
      }
    }
  }


  /** check the class files if they have undeterminism
    *
    * @param clz contract class files
    * @return either error messages, or the class files root path
    */
  def checkDeterminism(clz: Path): Either[Vector[String], Path] = {
    //check if the contract meta file exists
    val contract = Paths.get(clz.toString, "META-INF/contract").toFile
    if (!contract.isFile) Left(Vector("META-INF/contract not found"))
    else {
      val reader = new FileReader(contract)
      try{
        val br = new BufferedReader(reader)
        def readline0(acc: Vector[String]): Vector[String] =
          Option(br.readLine()) match {
            case None => acc
            case Some(l) => acc :+ l
          }
        val lines = readline0(Vector.empty)
        if(logger.isInfoEnabled) {
          lines.foreach(logger.info)
        }

        val track = CheckingClassLoader.ClassCheckingStatus()
        val ccl = new CheckingClassLoader(clz, track)

        lines.foreach { x =>
          x.split("#") match {
            case Array(c, m) =>
              ccl.findClass(c)
            case Array(c) =>
              ccl.findClass(c)
          }
        }

        if(track.isLegal) Right(clz)
        else Left(track.errors())


      } finally {
        reader.close()
      }
    }
  }

  private def allFiles(src: Path, suffix: String): Vector[String] = {
    def func0(_src: File, acc: Vector[String]): Vector[String] = {
      if (_src.isFile && _src.getAbsolutePath.endsWith(suffix)) acc :+ _src.getAbsolutePath
      else if (_src.isFile) acc
      else {
        _src.listFiles.toVector.foldLeft(acc) { (a, n) =>
          func0(n, a)
        }
      }
    }

    func0(src.toFile, Vector.empty)
  }

}

object Compiler extends Compiler
