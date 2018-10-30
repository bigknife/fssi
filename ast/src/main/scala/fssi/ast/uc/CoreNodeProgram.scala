package fssi.ast
package uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.base._
import fssi.types.biz._
import fssi.ast.uc.corenode._

import java.io._

trait CoreNodeProgram[F[_]] {

  /** Start up a core node.
    * @return node info
    */
  def startup(root: File, handler: JsonMessageHandler): SP[F, Node]

  /** Shutdown core node
    */
  def shutdown(node: Node): SP[F, Unit]

  /** handle transaction
    */
  def handleTransaction(transaction: Transaction): SP[F, Receipt]

  /** persist new block
    */
  def newBlockGenerated(block: Block): SP[F, Unit]
}

object CoreNodeProgram {
  def apply[F[_]](implicit M: blockchain.Model[F]): CoreNodeProgram[F] =
    new CoreNodeProgram[F] with StartupProgram[F] with ShutdownProgram[F]
    with HandleTransactionProgram[F] with NewBlockGeneratedProgram[F]{
      private[uc] val model: blockchain.Model[F] = M
    }
}
