package fssi
package tool

import java.io._

import types._
import io.circe._

sealed trait CmdArgs

object CmdArgs {

  object Empty extends CmdArgs

  /** CreateAccount Arguments
    */
  case class CreateAccountArgs(
      randomSeed: String = "GoodLuck",
      accountFile: File = new File(""),
      secretKeyFile: File = new File("")
  ) extends CmdArgs

  /** CreateChain Arguments
    */
  case class CreateChainArgs(dataDir: File, chainID: String) extends CmdArgs

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
      contractFile: File = new File(""),
      contractName: UniqueName = UniqueName.empty,
      contractVersion: Version = Version.empty
  ) extends CmdArgs

  /** Create Run Contract Transaction Arguments
    */
  case class CreateRunContractTransactionArgs(
      accountFile: File = new File(""),
      password: Array[Byte] = Array.emptyByteArray,
      contractName: UniqueName = UniqueName.empty,
      contractVersion: Version = Version.empty,
      method: Contract.Method = Contract.Method.empty,
      parameter: Contract.Parameter = Contract.Parameter.PEmpty
  ) extends CmdArgs

  /** Compile Contract Args
    */
  case class CompileContractArgs(
      projectDirectory: File = new File(""),
      outputFile: File = new File(""),
      sandboxVersion: CompileContractArgs.SandobxVersion =
        CompileContractArgs.SandobxVersion.`1.0.0`
  ) extends CmdArgs

  object CompileContractArgs {
    sealed trait SandobxVersion
    object SandobxVersion {
      case object `1.0.0` extends SandobxVersion {
        override def toString: String = "1.0.0"
      }

      def apply(s: String): SandobxVersion = s match {
        case "1.0.0" => `1.0.0`
        case _       => `1.0.0`
      }
    }
  }
}
