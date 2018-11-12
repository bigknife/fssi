package fssi.ast.uc
package edgenode
import bigknife.sop._
import bigknife.sop.implicits._
import fssi.types.biz.Message.ClientMessage
import fssi.types.biz.Transaction

trait HandleClientMessageProgram[F[_]] extends EdgeNodeProgram[F] with BaseProgram[F] {
  import model._

  /** handle client message
    * @param clientMessage message such as sponsor transaction
    */
  def handleClientMessage(clientMessage: ClientMessage): SP[F, Transaction] = {
    for {
      _                 <- network.broadcastMessage(clientMessage)
      transactionEither <- network.waitForMessageResponse(clientMessage)
      transaction       <- err.either(transactionEither)
    } yield transaction
  }
}
