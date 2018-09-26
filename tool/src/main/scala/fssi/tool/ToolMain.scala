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
        case CreateChainArgs(rootDir, chainID) => createChain(rootDir, chainID)
        case CreateTransferTransactionArgs(accountFile, secretKeyFile, payee, token, o) =>
          createTransferTransaction(accountFile, secretKeyFile, payee, token, o)
        case CompileContractArgs(projectDirectory, outputDirectory, sandboxVersion) =>
          compileContract(projectDirectory, outputDirectory, sandboxVersion.toString)
        case CreateDeployTransactionArgs(accountFile, secretKeyFile, contractFile, o) =>
          createDeployTransaction(accountFile, secretKeyFile, contractFile, o)
        case CreateRunTransactionArgs(accountFile,
                                      password,
                                      contractName,
                                      contractVersion,
                                      method,
                                      parameter,
                                      o) =>
          createRunTransaction(accountFile,
                               password,
                               contractName,
                               contractVersion,
                               method,
                               parameter,
                               o)

        case _ =>
      }
    case None =>
  }
}
