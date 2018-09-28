package fssi
package contract
package scaffold
package file

import java.nio._
import java.nio.file._
import fssi.types.exception._
import inf.BaseLogger
import scala.util._

class ContractFile extends BaseLogger {

  def createDefaultFiles(projectRoot: Path): Either[FSSIException, Unit] = {
    Try {
      require(projectRoot.toFile.exists && projectRoot.toFile.isDirectory,
              "contract project root must be a directory")

      //create readme
      val sampleReadmeString =
        better.files.Resource.getAsString("META-INF/README.md")(charset.Charset.forName("utf-8"))
      val readmeFilePath = Paths.get(projectRoot.toString, "README.md")
      readmeFilePath.toFile.createNewFile()
      better.files.File(readmeFilePath).overwrite(sampleReadmeString)

      // create default contract meta file
      val sampleConfString = better.files.Resource.getAsString("META-INF/meta-sample.conf")(
        charset.Charset.forName("utf-8"))
      val metaFilePath =
        Paths.get(projectRoot.toString, "src/main/resources/META-INF/contract.meta.conf")
      if (!metaFilePath.toFile.getParentFile.exists()) metaFilePath.toFile.getParentFile.mkdirs()
      metaFilePath.toFile.createNewFile()
      better.files.File(metaFilePath).overwrite(sampleConfString)
      logger.debug(s"created sample meta conf file: $metaFilePath")

      // create contract interface template
      val sampleInterfaceString = better.files.Resource.getAsString(
        "META-INF/InterfaceSample.java")(charset.Charset.forName("utf-8"))
      val interfaceFilePath =
        Paths.get(projectRoot.toString, "src/main/java/com/fssi/sample/InterfaceSample.java")
      if (!interfaceFilePath.toFile.getParentFile.exists())
        interfaceFilePath.toFile.getParentFile.mkdirs()
      interfaceFilePath.toFile.createNewFile()
      better.files.File(interfaceFilePath).overwrite(sampleInterfaceString)
      logger.debug(s"created sample interface java file: $interfaceFilePath")
    }.toEither.left.map(x => new FSSIException(x.getMessage))
  }
}
