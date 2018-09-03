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
        case CreateAccountArgs(password)       => createAccount(password)
        case CreateChainArgs(dataDir, chainID) => createChain(dataDir, chainID)
        case CreateTransferTransactionArgs(accountFile, password, payee, token) =>
          createTransferTransaction(accountFile, password, payee, token)
        case _ =>
      }
    case None =>
  }
}
