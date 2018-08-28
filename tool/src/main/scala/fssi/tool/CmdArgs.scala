package fssi
package tool

import java.io._

import fssi.types.CodeFormat

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

}
