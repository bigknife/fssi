package fssi
package tool

import java.io._

import fssi.types.OutputFormat

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
                                 outputFormat: OutputFormat = OutputFormat.Jar)
      extends CmdArgs

}
