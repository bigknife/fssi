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
        case CreateTransferTransactionArgs(accountFile, secretKeyFile, payee, token) =>
          createTransferTransaction(accountFile, secretKeyFile, payee, token)
        case CompileContractArgs(projectDirectory, outputDirectory, sandboxVersion) =>
          compileContract(projectDirectory, outputDirectory, sandboxVersion.toString)
        case CreateDeployTransactionArgs(accountFile, secretKeyFile, contractFile) =>
          createDeployTransaction(accountFile, secretKeyFile, contractFile)
        case CreateRunTransactionArgs(accountFile,
                                      password,
                                      contractName,
                                      contractVersion,
                                      method,
                                      parameter) =>
          createRunTransaction(accountFile,
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
