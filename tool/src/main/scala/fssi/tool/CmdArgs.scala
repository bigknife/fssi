package fssi
package tool

import java.io._

import types.biz._
import types.base._
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
  case class CreateChainArgs(rootDir: File, chainID: String) extends CmdArgs

  case object CreateTransactionArgsPlaceHolder extends CmdArgs

  /** Create Transfer Transaction Arguments
    */
  case class CreateTransferTransactionArgs(
      accountFile: File = new File(""),
      secretKeyFile: File = new File(""),
      payee: Account.ID = Account.emptyId,
      token: Token = Token.Zero,
      outputFile: Option[File] = None
  ) extends CmdArgs

  /** Create Publish Contract Transaction Arguments
    */
  case class CreateDeployTransactionArgs(
      accountFile: File = new File(""),
      secretKeyFile: File = new File(""),
      contractFile: File = new File(""),
      outputFile: Option[File] = None
  ) extends CmdArgs

  /** Create Run Contract Transaction Arguments
    */
  case class CreateRunTransactionArgs(
      accountFile: File = new File(""),
      secretKeyFile: File = new File(""),
      contractName: UniqueName = UniqueName.empty,
      contractVersion: Contract.Version = Contract.Version.empty,
      methodAlias: String = "",
      parameter: Option[Contract.UserContract.Parameter] = None,
      outputFile: Option[File] = None
  ) extends CmdArgs

  /** Compile Contract Args
    */
  case class CompileContractArgs(
      accountFile: File = new File(""),
      secretKeyFile: File = new File(""),    
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
