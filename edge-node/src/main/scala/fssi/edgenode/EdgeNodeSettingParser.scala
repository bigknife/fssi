package fssi
package edgenode

import scopt._
import interpreter._
import types._

object EdgeNodeSettingParser extends OptionParser[Setting.EdgeNodeSetting]("edgenode") {
  head("edgenode", "0.1.0")
  help("help").abbr("h").text("print this help messages")

  opt[java.io.File]("working-dir")
    .abbr("w")
    .text("working directory")
    .action((x, c) => c.copy(workingDir = x))
}
