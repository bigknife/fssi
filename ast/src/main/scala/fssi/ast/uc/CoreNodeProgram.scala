package fssi
package ast
package uc

import utils._
import types._, exception._
import types.syntax._
import bigknife.sop._
import bigknife.sop.implicits._

trait CoreNodeProgram[F[_]] extends BaseProgram[F] {
  import model._

  /** Start up a core node.
    * @return node info
    */
  def startup(handler: JsonMessageHandler): SP[F, Node] =
    for {
      n1 <- network.startupP2PNode(handler)
      n2 <- network.bindAccount(n1)
      _ <- network.setCurrentNode(n2)
      _  <- consensusEngine.initialize(n2.account.get)
      //TODO: some other components should be initialized here
    } yield n2

  /** Shutdown core node
    */
  def shutdown(node: Node): SP[F, Unit] =
    for {
      _ <- network.shutdownP2PNode(node)
    } yield ()

  /** handle transaction
    */
  def handleTransaction(transaction: Transaction): SP[F, Unit] = {
    import transactionService._
    import blockStore._
    import crypto._
    import consensusEngine._
    import network._
    for {
      toBeSingedBytes <- calculateSingedBytesOfTransaction(transaction)
      verified <- verifySignature(toBeSingedBytes,
                                  transaction.publicKeyForVerifing,
                                  transaction.signature)

      _ <- failM(
        verified,
        new FSSIException(s"Transaction of id=${transaction.id} signature can't be verified"))
      determinedBlock    <- getLatestDeterminedBlock()
      undetermindedBlock <- appendTransactionToUnDeterminedBlock(determinedBlock, transaction)
      node               <- network.getCurrentNode()
      _                  <- tryToAgreeBlock(node.account.get, determinedBlock, undetermindedBlock)
    } yield ()
  }
}
object CoreNodeProgram {
  def apply[F[_]](implicit M: components.Model[F]): CoreNodeProgram[F] = new CoreNodeProgram[F] {
    val model: components.Model[F] = M
  }
}
