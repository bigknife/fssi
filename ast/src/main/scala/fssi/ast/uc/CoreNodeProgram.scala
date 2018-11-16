package fssi.ast
package uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types._
import fssi.types.base._
import fssi.types.biz._
import fssi.ast.uc.corenode._

import java.io._

trait CoreNodeProgram[F[_]] {

  /** Start up a full-functioning core node.
    * runing consensus node and application node
    * @return node info
    */
  def startupFull(root: File,
                  consensusMessageHandler: Message.Handler[ConsensusMessage, Unit],
                  applicationMessageHandler: Message.Handler[ApplicationMessage, Unit])
    : SP[F, (Node.ConsensusNode, Node.ApplicationNode)]

  /** Start up a semi-functioning core node.
    * runing consensus node only
    * @return node info
    */
  def startupSemi(
      root: File,
      consensusMessageHandler: Message.Handler[ConsensusMessage, Unit]): SP[F, Node.ConsensusNode]

  /** Shutdown core node
    */
  def shutdown(consensusNode: Node.ConsensusNode,
               applicationNode: Option[Node.ApplicationNode]): SP[F, Unit]

  /** handle transaction
    */
  def handleTransaction(transaction: Transaction): SP[F, Receipt]

  /** persist new block
    */
  def newBlockGenerated(block: Block): SP[F, Unit]

  /** process consensus message
    */
  def processConsensusMessage(message: ConsensusMessage): SP[F, Unit]

  /** process application message
    */
  def processApplicationMessage(applicationMessage: ApplicationMessage): SP[F, Unit]

  /** broadcast consensus message
    */
  def broadcastConsensusMessage(consensusMessage: ConsensusMessage): SP[F, Unit]
}

object CoreNodeProgram {

  private final class Implementation[G[_]](implicit M: blockchain.Model[G])
      extends CoreNodeProgram[G]
      with StartupProgram[G]
      with ShutdownProgram[G]
      with HandleTransactionProgram[G]
      with NewBlockGeneratedProgram[G]
      with ProcessMessageProgram[G] {

    private[uc] val model: blockchain.Model[G] = M

  }

  def apply[G[_]](implicit M: blockchain.Model[G]): CoreNodeProgram[G] = new Implementation[G]

  def instance: CoreNodeProgram[blockchain.Model.Op] = apply[blockchain.Model.Op]
}
