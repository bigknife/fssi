package fssi
package tool

import scopt._
import CmdArgs._
import types.biz._
import types.base._
import interpreter.jsonCodecs._

object CmdArgsParser extends OptionParser[CmdArgs]("fssitool") {

  head("tool", "0.2")
  help("help").abbr("h").text("print this help messages")

  cmd("CreateAccount")
    .action((_, _) => CreateAccountArgs())
    .text("Create An FSSI Account")
    .children(
      opt[String]("random-seed")
        .text("a random string to create secret key for account")
        .abbr("s")
        .required()
        .action((x, c) => c.asInstanceOf[CreateAccountArgs].copy(randomSeed = x)),
      opt[java.io.File]("account-file")
        .text("output account json file path")
        .abbr("af")
        .required()
        .action((x, c) => c.asInstanceOf[CreateAccountArgs].copy(accountFile = x)),
      opt[java.io.File]("key-file")
        .text("output secret key file")
        .abbr("kf")
        .required()
        .action((x, c) => c.asInstanceOf[CreateAccountArgs].copy(secretKeyFile = x))
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
    .text("Compile Smart Contract Project")
    .action((_, _) => CompileContractArgs())
    .children(
      opt[java.io.File]("project-directory")
        .abbr("pd")
        .text("smart contract project root path")
        .required()
        .action((x, c) => c.asInstanceOf[CompileContractArgs].copy(projectDirectory = x)),
      opt[java.io.File]("output-file")
        .abbr("of")
        .text("the compiled artifact file name, with absolute path")
        .required()
        .action((x, c) => c.asInstanceOf[CompileContractArgs].copy(outputFile = x)),
      opt[String]("sandbox-version")
        .abbr("sv")
        .text("supported version of the sandbox on which the smart contract will run, default is 1.0.0(only support 1.0.0 now)")
        .action((x, c) =>
          c.asInstanceOf[CompileContractArgs]
            .copy(sandboxVersion = CompileContractArgs.SandobxVersion(x)))
    )

  cmd("CreateTransaction")
    .text("Create Transaction")
    .children(
      cmd("transfer")
        .text("create transfer transaction")
        .action((_, _) => CreateTransferTransactionArgs())
        .children(
          opt[java.io.File]("account-file")
            .abbr("af")
            .required()
            .text("payer's account file created by 'CreateAccount'")
            .action((f, c) => c.asInstanceOf[CreateTransferTransactionArgs].copy(accountFile = f)),
          opt[java.io.File]("key-file")
            .abbr("kf")
            .required()
            .text("payer's account secret key file")
            .action((x, c) =>
              c.asInstanceOf[CreateTransferTransactionArgs]
                .copy(secretKeyFile = x)),
          opt[String]("payee-id")
            .abbr("pi")
            .required()
            .text("payee's account id, the hex string of it's public key")
            .action((x, c) =>
              c.asInstanceOf[CreateTransferTransactionArgs]
                .copy(payee = Account.ID(BytesValue.decodeBcBase58(x).get.bytes))),
          opt[String]("token")
            .abbr("t")
            .required()
            .text("amount to be transfered, in form of 'number' + 'unit', eg. 100Sweet. ")
            .action((x, c) =>
              c.asInstanceOf[CreateTransferTransactionArgs].copy(token = Token.parse(x))),
          opt[java.io.File]("output-file")
            .abbr("o")
            .text("if set, the message will output to this file")
            .action((x, c) =>
              c.asInstanceOf[CreateTransferTransactionArgs].copy(outputFile = Some(x)))
        ),
      cmd("deploy")
        .text("create deploy contract transaction")
        .action((_, _) => CreateDeployTransactionArgs())
        .children(
          opt[java.io.File]("account-file")
            .abbr("af")
            .required()
            .text("contract owner's account file created by 'CreateAccount'")
            .action((f, c) => c.asInstanceOf[CreateDeployTransactionArgs].copy(accountFile = f)),
          opt[java.io.File]("key-file")
            .abbr("kf")
            .required()
            .text("contract owner's account secret key file")
            .action((x, c) =>
              c.asInstanceOf[CreateDeployTransactionArgs]
                .copy(secretKeyFile = x)),
          opt[java.io.File]("contract-file")
            .abbr("cf")
            .required()
            .text("smart contract file")
            .action((x, c) => c.asInstanceOf[CreateDeployTransactionArgs].copy(contractFile = x)),
          opt[java.io.File]("output-file")
            .abbr("o")
            .text("if set, the message will output to this file")
            .action(
              (x, c) => c.asInstanceOf[CreateDeployTransactionArgs].copy(outputFile = Some(x)))
        ),
      cmd("run")
        .text("create run contract transaction")
        .action((_, _) => CreateRunTransactionArgs())
        .children(
          opt[java.io.File]("account-file")
            .abbr("af")
            .required()
            .text("contract caller's account file created by 'CreateAccount'")
            .action((f, c) => c.asInstanceOf[CreateRunTransactionArgs].copy(accountFile = f)),
          opt[java.io.File]("key-file")
            .abbr("kf")
            .required()
            .text("contract caller's account secret key file")
            .action((x, c) =>
              c.asInstanceOf[CreateRunTransactionArgs]
                .copy(secretKeyFile = x))
        ),
      opt[String]("contract-name")
        .abbr("name")
        .required()
        .text("calling contract name")
        .action((x, c) =>
          c.asInstanceOf[CreateRunTransactionArgs].copy(contractName = UniqueName(x))),
      opt[String]("contract-version")
        .abbr("version")
        .required()
        .text("the version of the invoking contract")
        .action((x, c) =>
          c.asInstanceOf[CreateRunTransactionArgs]
            .copy(contractVersion = Contract.Version(x).get)),
      opt[String]("method-alias")
        .abbr("m")
        .required()
        .text("alias of method to invoke")
        .action((x, c) => c.asInstanceOf[CreateRunTransactionArgs].copy(methodAlias = x)),
      opt[String]("parameter")
        .abbr("p")
        .text("parameters for this invoking")
        .action(
          (x, c) =>
            c.asInstanceOf[CreateRunTransactionArgs]
              .copy(parameter = Some(
                io.circe.parser.parse(x).right.get.as[Contract.UserContract.Parameter].right.get))),
      opt[java.io.File]("output-file")
        .abbr("o")
        .text("if set, the message will output to this file")
        .action((x, c) => c.asInstanceOf[CreateRunTransactionArgs].copy(outputFile = Some(x)))
    )
}
