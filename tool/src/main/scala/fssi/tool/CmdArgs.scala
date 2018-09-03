package fssi
package tool

import java.io._
import types._

sealed trait CmdArgs

object CmdArgs {

  object Empty extends CmdArgs

  /** CreateAccount Arguments
    */
  case class CreateAccountArgs(password: String = "GoodLuck") extends CmdArgs

  /** CreateChain Arguments
    */
  case class CreateChainArgs(dataDir: java.io.File, chainID: String) extends CmdArgs

  case object CreateTransactionArgsPlaceHolder extends CmdArgs

  /** Create Transfer Transaction Arguments
    */
  case class CreateTransferTransactionArgs(
      accountFile: File = new File(""),
      password: Array[Byte] = Array.emptyByteArray,
      payee: Account.ID = Account.ID(HexString.empty),
      token: Token = Token.Zero
  ) extends CmdArgs

  /** Create Publish Contract Transaction Arguments
    */
  case class CreatePublishContractTransactionArgs(
      accountFile: File = new File(""),
      password: Array[Byte] = Array.emptyByteArray,
      contractFile: File = new File("")
  ) extends CmdArgs

  /** Compile Contract Args
    */
  case class CompileContractArgs(
      projectDirectory: File = new File(""),
      outputFile: File = new File(""),
      sandboxVersion: String = "0.0.1"
  ) extends CmdArgs
}
