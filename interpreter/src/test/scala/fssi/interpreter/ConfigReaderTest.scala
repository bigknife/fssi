package fssi
package interpreter
import java.io.File

import org.scalatest.FunSuite
import Configuration._

class ConfigReaderTest extends FunSuite {

  test("test configuration") {
    val path                         = getClass.getClassLoader.getResource("META-INF/config-test.conf").getFile
    val file: File                   = new File(path)
    val configuration: Configuration = file.asConfiguration
    info(s"$configuration")
    val coreNodeConfig = file.asCoreNodeConfig
    info(s"$coreNodeConfig")
    val edgeNodeConfig = file.asEdgeNodeConfig
    info(s"$edgeNodeConfig")
  }
}
