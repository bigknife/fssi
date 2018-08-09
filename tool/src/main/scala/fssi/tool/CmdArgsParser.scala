package fssi
package tool

import scopt._
import CmdArgs._

object CmdArgsParser extends OptionParser[CmdArgs]("fssitool") {

  head("fssitool", "0.1.0")
  help("help").abbr("h").text("print this help messages")

  cmd("CreateAccount")
    .action((_, _) => CreateAccountArgs())
    .text("Create An FSSI Account")
    .children(
      opt[String]("password")
        .abbr("p")
        .required()
        .action((x, c) => c.asInstanceOf[CreateAccountArgs].copy(password = x))
    )
}