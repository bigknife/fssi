package fssi
package contract
package scaffold
package directory

import java.nio.file._

import fssi.contract.scaffold.inf.BaseLogger
import fssi.types.exception._
import fssi.utils._

import scala.util._

class ContractDirectory extends BaseLogger {

  def createDefaultDirectory(rootPath: Path): Either[FSSIException, Unit] = {
    Try {
      if (rootPath.toFile.exists) FileUtil.deleteDir(rootPath)
      rootPath.toFile.mkdirs()
      logger.debug(s"created rootPath dir: $rootPath")

      // create lib and src folders
      val libPath = Paths.get(rootPath.toString, "lib")
      libPath.toFile.mkdirs
      logger.debug(s"created lib dir: $libPath")
      val srcPath = Paths.get(rootPath.toString, "src")
      srcPath.toFile.mkdirs
      logger.debug(s"created src dir: $srcPath")

      // create src/main 、src/test folders
      val mainPath = Paths.get(srcPath.toString, "main")
      mainPath.toFile.mkdirs
      logger.debug(s"created main dir: $mainPath")
      val testPath = Paths.get(srcPath.toString, "test")
      testPath.toFile.mkdirs
      logger.debug(s"created test dir: $testPath")

      // create src/main/java 、src/main/resources/META-INF folders
      val mainJavaPath = Paths.get(mainPath.toString, "java/com/fssi/sample")
      mainJavaPath.toFile.mkdirs
      logger.debug(s"created main java dir: $mainJavaPath")
      val mainResourcesPath = Paths.get(mainPath.toString, "resources/META-INF")
      mainResourcesPath.toFile.mkdirs
      logger.debug(s"created main resources dir: $mainResourcesPath")

      // create src/test/java 、src/test/resources/META-INF folders
      val testJavaPath = Paths.get(testPath.toString, "java/com/fssi/sample")
      testJavaPath.toFile.mkdirs
      logger.debug(s"created test java dir: $testJavaPath")
      val testResourcesPath = Paths.get(testPath.toString, "resources/META-INF")
      testResourcesPath.toFile.mkdirs
      logger.debug(s"created test resources dir: $testResourcesPath")
    }.toEither.left.map(x => new FSSIException(x.getMessage))
  }
}
