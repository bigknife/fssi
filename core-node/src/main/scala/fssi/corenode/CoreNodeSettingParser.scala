package fssi
package corenode

import scopt._
import interpreter._
import types._

object CoreNodeSettingParser extends OptionParser[Setting.CoreNodeSetting]("corenode") {
  head("corenode", "0.2.0")
  help("help").abbr("h").text("print this help messages")

  opt[java.io.File]("working-dir")
    .abbr("w")
    .text("working directory")
    .action((x, c) => c.copy(workingDir = x))
}
