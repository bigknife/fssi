package fssi
package tool

import scopt._
import CmdArgs._
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
