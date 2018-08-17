package fssi
package tool

import java.io.File

import scopt._
import CmdArgs._
import fssi.types.OutputFormat

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

  cmd("CreateChain")
    .action((_, _) => CreateChainArgs(new java.io.File("."), "testnet"))
    .text("Create a chain")
    .children(
      opt[java.io.File]("data-dir")
        .abbr("d")
        .required()
        .action((x, c) => c.asInstanceOf[CreateChainArgs].copy(dataDir = x)),
      opt[String]("chain-id")
        .abbr("id")
        .required()
        .action((x, c) => c.asInstanceOf[CreateChainArgs].copy(chainID = x))
    )

  cmd("CompileContract")
    .text("Compile smart contract")
    .action((_, _) ⇒ CompileContractArgs(new File("."), new File(".")))
    .children(
      opt[java.io.File]("source-code")
        .abbr("f")
        .required()
        .validate(f ⇒
          if (f.exists() && f.isDirectory) Right(()) else Left(s"dir ${f.getPath} not found"))
        .action((f, c) ⇒ c.asInstanceOf[CompileContractArgs].copy(projectDir = f)),
      opt[java.io.File]("output")
        .abbr("o")
        .required()
        .action((f, c) ⇒ c.asInstanceOf[CompileContractArgs].copy(targetDir = f)),
      opt[String]("format")
        .abbr("t")
        .optional()
        .action((t, c) ⇒ c.asInstanceOf[CompileContractArgs].copy(outputFormat = OutputFormat(t)))
    )
}
