package fssi
package tool

import CmdArgs._
import handler._

/** FSSI TOOL Main
  */
object ToolMain extends App {

  CmdArgsParser.parse(args, Empty) match {
    case Some(result) =>
      result match {
        case CreateAccountArgs(randomSeed, accountFile, secretKeyFile) =>
          createAccount(randomSeed, accountFile, secretKeyFile)
        case CreateChainArgs(dataDir, chainID) => createChain(dataDir, chainID)
        case CreateTransferTransactionArgs(accountFile, password, payee, token) =>
          createTransferTransaction(accountFile, password, payee, token)
        case CompileContractArgs(projectDirectory, outputDirectory, sandboxVersion) =>
          compileContract(projectDirectory, outputDirectory, sandboxVersion.toString)
        case CreatePublishContractTransactionArgs(accountFile,
                                                  password,
                                                  contractFile,
                                                  contractName,
                                                  contractVersion) =>
          createPublishContractTransaction(accountFile,
                                           password,
                                           contractFile,
                                           contractName,
                                           contractVersion)
        case CreateRunContractTransactionArgs(accountFile,
                                              password,
                                              contractName,
                                              contractVersion,
                                              method,
                                              parameter) =>
          createRunContractTransaction(accountFile,
                                       password,
                                       contractName,
                                       contractVersion,
                                       method,
                                       parameter)
        case _ =>
      }
    case None =>
  }
}
