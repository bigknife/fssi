package fssi.sandbox

import _root_.java.nio.file._

import org.slf4j.LoggerFactory
import fssi.sandbox.misc.{ClassNameUtil => CNU}

import scala.util._
import scala.collection.immutable

trait WhitelistClassLoader extends ClassLoader {
  private val logger                                         = LoggerFactory.getLogger(classOf[WhitelistClassLoader])
  private val loadedClasses: immutable.Map[String, Class[_]] = immutable.Map.empty

  // primary class path
  val primaryClasspathSearchPath: Vector[Path]

  // file system search path
  val fileSystemSearchPath: Vector[Path]

  override def findClass(qualifiedClassName: String): Class[_] = {

    def findClass0(): Either[Throwable, Class[_]] =
      Try {
        super.findClass(qualifiedClassName)
      } match {
        case Success(cls) => Right(cls)
        case Failure(t)   =>
          // find the class ourselves
          val classInternalName = CNU.convertQualifiedClassNameToInternalForm(qualifiedClassName)
          val classDir = locateClassfileDir(classInternalName)
          // read the class file, remove non-deterministic methods, inject instument cost byte codes.
          //   this process is called `asContract`
          ???
      }

    loadedClasses
      .get(qualifiedClassName)
      .orElse(loadedClasses.get(CNU.sandboxQualifiedTypedName(qualifiedClassName))) match {
      case Some(cls) => cls
      case None =>
        findClass0() match {
          case Left(t) =>
            throw new ClassNotFoundException(s"Class $qualifiedClassName could not be loaded", t)
          case Right(cls) => cls
        }
    }
  }

  private def locateClassfileDir(internalClassName: String): Either[Throwable, Path] = {
    // first find from primary class paths
    primaryClasspathSearchPath.find(x =>
      Files.isRegularFile(Paths.get(x.toString, s"$internalClassName.class"))) match {
      case Some(p) if Files.isReadable(p) => Right(p)
      case Some(p)                        => Left(new IllegalArgumentException(s"File $p found but is not readable"))
      case None                           =>
        // second find from file system search path
        fileSystemSearchPath.find(x => Files.isRegularFile(x.resolve(s"$internalClassName.class"))) match {
          case Some(p) if Files.isReadable(p) => Right(p)
          case Some(p)                        => Left(new IllegalArgumentException(s"File $p found but is not readable"))
          case None =>
            throw new ClassNotFoundException(
              s"Requested class ${CNU.convertInternalFromToQualifiedClassName(internalClassName)} could not be found")
        }

    }

  }

  private def asContract(clazz: Path): Either[Throwable, Array[Byte]] = ???

}
