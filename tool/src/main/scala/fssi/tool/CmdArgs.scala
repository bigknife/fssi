package fssi
package tool

import java.io._

import fssi.types.CodeFormat
import fssi.types.Contract.Parameter.PrimaryParameter
import types._

sealed trait CmdArgs

object CmdArgs {

  object Empty extends CmdArgs

  /** CreateAccount Arguments
    */
  case class CreateAccountArgs(password: String = "GoodLuck") extends CmdArgs

  /** CreateChain Arguments
    */
  case class CreateChainArgs(dataDir: File, chainID: String) extends CmdArgs

  /***
    * CompileContract Arguments
    */
  case class CompileContractArgs(projectDir: File,
                                 targetDir: File,
                                 outputFormat: CodeFormat = CodeFormat.Jar)
      extends CmdArgs

  case class RunContractArgs(contractFile: File,
                             qualifiedClass: String,
                             methodName: String,
                             parameters: Array[String],
                             decodeFormat: CodeFormat = CodeFormat.Jar)
      extends CmdArgs

  case object CreateTransactionArgsPlaceHolder extends CmdArgs

  /** Create Transfer Transaction Arguments
    */
  case class CreateTransferTransactionArgs(
    accountFile: File,
    password: Array[Byte],
    payee: Account.ID,
    token: Token
  ) extends CmdArgs

  object CreateTransferTransactionArgs {
    def empty: CreateTransferTransactionArgs = CreateTransferTransactionArgs(
      accountFile = new File(""),
      password = Array.emptyByteArray,
      payee = Account.ID(HexString.empty),
      token = Token.Zero
    )
  }
}
