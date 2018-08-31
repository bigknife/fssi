package fssi
package tool

import CmdArgs._
import fssi.types.Contract.Parameter.PrimaryParameter
import handler._

/** FSSI TOOL Main
  */
object ToolMain extends App {

  CmdArgsParser.parse(args, Empty) match {
    case Some(result) =>
      result match {
        case CreateAccountArgs(password)       => createAccount(password)
        case CreateChainArgs(dataDir, chainID) => createChain(dataDir, chainID)
        case CompileContractArgs(projectDir, targetDir, format) =>
          compileContract(projectDir, targetDir, format)
        case RunContractArgs(classesDir, qualifiedClass, methodName, parameters, decodeFormat) ⇒
          runContract(classesDir,
                      qualifiedClass,
                      methodName,
                      parameters.map(PrimaryParameter(_)),
                      decodeFormat)
        case _ =>
      }
    case None =>
  }
}
