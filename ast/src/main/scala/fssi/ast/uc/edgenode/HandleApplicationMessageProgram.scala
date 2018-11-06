package fssi.ast.uc
package edgenode
import bigknife.sop._
import bigknife.sop.implicits._
import fssi.types.biz.Message.ApplicationMessage

trait HandleApplicationMessageProgram[F[_]] extends EdgeNodeProgram[F] with BaseProgram[F] {
  import model._

  /** handle application message
    * @param applicationMessage message such as query
    */
  def handleApplicationMessage(applicationMessage: ApplicationMessage): SP[F, Unit] =
    network.broadcastMessage(applicationMessage)
}
