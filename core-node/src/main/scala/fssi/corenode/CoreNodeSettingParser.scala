package fssi
package corenode

import scopt._
import interpreter._
import types._

object CoreNodeSettingParser extends OptionParser[Setting.CoreNodeSetting]("corenode") {
  head("corenode", "0.1.0")
  help("help").abbr("h").text("print this help messages")

  opt[java.io.File]("working-dir")
    .abbr("w")
    .text("working directory")
    .action((x, c) => c.copy(workingDir = x))

  opt[String]("password")
    .abbr("p")
    .required()
    .text("password of the bound account")
    .action((x, c) => c.copy(password = x.getBytes("utf-8")))
}
