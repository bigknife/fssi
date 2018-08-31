package fssi
package tool

import java.io.File

import scopt._
import CmdArgs._
import fssi.types.CodeFormat
import types._

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
        .text("dir of contract source code")
        .required()
        .validate(f ⇒
          if (f.exists() && f.isDirectory) Right(()) else Left(s"dir ${f.getPath} not found"))
        .action((f, c) ⇒ c.asInstanceOf[CompileContractArgs].copy(projectDir = f)),
      opt[java.io.File]("output")
        .abbr("o")
        .text("dir of compiled contract clazz")
        .required()
        .action((f, c) ⇒ c.asInstanceOf[CompileContractArgs].copy(targetDir = f)),
      opt[String]("encode-format")
        .abbr("ef")
        .text("encode format for compiled contract result,support jar、hex and base64. default jar")
        .optional()
        .action((t, c) ⇒ c.asInstanceOf[CompileContractArgs].copy(outputFormat = CodeFormat(t)))
    )

  cmd("RunContract")
    .text("run smart contract")
    .action((_, _) ⇒ RunContractArgs(new File("."), "", "", Array.empty[String]))
    .children(
      opt[java.io.File]("compiled-contract-file")
        .abbr("ccf")
        .text("file of compiled contract")
        .required()
        .validate(f ⇒
          if (f.exists() && f.isFile) Right(()) else Left(s"file ${f.getPath} not found"))
        .action((f, c) ⇒ c.asInstanceOf[RunContractArgs].copy(contractFile = f)),
      opt[String]("class-name")
        .abbr("cn")
        .required()
        .text("qualified class name expose in contract configuration")
        .action((n, c) ⇒ c.asInstanceOf[RunContractArgs].copy(qualifiedClass = n)),
      opt[String]("method-name")
        .abbr("mn")
        .required()
        .text("method name be invoked in class exposed in contract configuration")
        .action((m, c) ⇒ c.asInstanceOf[RunContractArgs].copy(methodName = m)),
      opt[String]("parameters")
        .abbr("p")
        .text("parameters for invoked method,comma split in multiple sense")
        .optional()
        .action((p, c) ⇒ c.asInstanceOf[RunContractArgs].copy(parameters = p.split(","))),
      opt[String]("decode-format")
        .abbr("df")
        .text("contract decode format,support jar、hex and base64. default jar")
        .optional()
        .action((df, c) ⇒ c.asInstanceOf[RunContractArgs].copy(decodeFormat = CodeFormat(df)))

  cmd("CreateTransaction")
    .text("Create Transaction")
    .children(
      cmd("transfer")
        .text("create transfer transaction")
        .action((_, _) => CreateTransferTransactionArgs.empty)
        .children(
          opt[java.io.File]("account-file")
            .abbr("af")
            .required()
            .text("payer account file created by 'CreateAccount'")
            .action((f, c) => c.asInstanceOf[CreateTransferTransactionArgs].copy(accountFile = f)),
          opt[String]("password")
            .abbr("p")
            .required()
            .text("payer's account password")
            .action((x, c) =>
              c.asInstanceOf[CreateTransferTransactionArgs].copy(password = x.getBytes("utf-8"))),
          opt[String]("payee-id")
            .abbr("pi")
            .required()
            .text("payee's account id, the hex string of it's public key")
            .action((x, c) =>
              c.asInstanceOf[CreateTransferTransactionArgs]
                .copy(payee = Account.ID(HexString.decode(x)))),
          opt[String]("token")
            .abbr("t")
            .required()
            .text("amount to be transfered, in form of 'number' + 'unit', eg. 100Sweet. ")
            .action((x, c) => c.asInstanceOf[CreateTransferTransactionArgs].copy(token = Token.parse(x)))
        )
    )
}
