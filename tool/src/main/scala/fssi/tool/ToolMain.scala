package fssi
package tool

import CmdArgs._
import fssi.interpreter.{Setting, StackConsoleMain}
import handler._

/** FSSI TOOL Main
  */
object ToolMain extends StackConsoleMain[CmdArgs] {

  override def cmdArgs(xs: Array[String]): Option[CmdArgs] = CmdArgsParser.parse(args, Empty)
  override def program(cmdArgs: Option[CmdArgs], setting: Setting): StackConsoleMain.Effect =
    cmdArgs match {
      case Some(result) =>
        result match {
          case CreateAccountArgs(randomSeed, accountFile, secretKeyFile) =>
            createAccount(randomSeed, accountFile, secretKeyFile)
          case CreateChainArgs(rootDir, chainID) =>
            createChain(rootDir, chainID)
          case CreateContractProjectArgs(projectDir) =>
            createContractProject(projectDir)
          case CreateTransferTransactionArgs(accountFile,
                                             secretKeyFile,
                                             payee,
                                             token,
                                             outputFile) =>
            createTransferTransaction(accountFile, secretKeyFile, payee, token, outputFile)
          case CreateDeployTransactionArgs(accountFile, secretKeyFile, contractFile, outputFile) =>
            createDeployTransaction(accountFile, secretKeyFile, contractFile, outputFile)
          case CreateRunTransactionArgs(accountFile,
                                        secretKeyFile,
                                        contractName,
                                        contractVersion,
                                        methodAlias,
                                        parameter,
                                        outputFile) =>
            createRunTransaction(accountFile,
                                 secretKeyFile,
                                 contractName,
                                 contractVersion,
                                 methodAlias,
                                 parameter,
                                 outputFile)
          case CompileContractArgs(accountFile,
                                   secretKeyFile,
                                   projectDirectory,
                                   outputFile,
                                   sandboxVersion) =>
            compileContract(accountFile,
                            secretKeyFile,
                            projectDirectory,
                            outputFile,
                            sandboxVersion.toString)
        }
      case _ =>
    }
}
