package fssi.ast.uc
package edgenode
import bigknife.sop._
import bigknife.sop.implicits._
import fssi.types.biz.Message.ClientMessage

trait HandleClientMessageProgram[F[_]] extends EdgeNodeProgram[F] with BaseProgram[F] {

  /** handle client message
    * @param clientMessage message such as sponsor transaction
    */
  def handleClientMessage(clientMessage: ClientMessage): SP[F, Unit] = ???
}
