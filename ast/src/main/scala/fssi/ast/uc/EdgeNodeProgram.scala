package fssi.ast
package uc
import fssi.types.biz.{Message, Transaction}
import fssi.types.biz.Message.{ApplicationMessage, ClientMessage}
import bigknife.sop._
import bigknife.sop.implicits._
import fssi.ast.uc.edgenode.{
  HandleApplicationMessageProgram,
  HandleClientMessageProgram,
  ShutdownProgram,
  StartupProgram
}
import fssi.types.ServiceResource
import fssi.types.biz.Node.{ApplicationNode, ServiceNode}

trait EdgeNodeProgram[F[_]] {

  /** start up a edge node
    * @param applicationMessageHandler handler to handle application query message
    * @param clientMessageHandler handler to handle client json rpc message
    */
  def startup(applicationMessageHandler: Message.Handler[ApplicationMessage],
              clientMessageHandler: Message.Handler[ClientMessage],
              serviceResource: ServiceResource): SP[F, (ApplicationNode, ServiceNode)]

  /** handle application message
    * @param applicationMessage message such as query
    */
  def handleApplicationMessage(applicationMessage: ApplicationMessage): SP[F, Unit]

  /** handle client message
    * @param clientMessage message such as sponsor transaction
    */
  def handleClientMessage(clientMessage: ClientMessage): SP[F, Transaction]

  def shutdown(applicationNode: ApplicationNode, serviceNode: ServiceNode): SP[F, Unit]
}

object EdgeNodeProgram {

  final class Implementation[G[_]](implicit M: blockchain.Model[G])
      extends EdgeNodeProgram[G]
      with StartupProgram[G]
      with HandleApplicationMessageProgram[G]
      with HandleClientMessageProgram[G]
      with ShutdownProgram[G] {
    override private[uc] val model = M
  }

  def apply[G[_]](implicit M: blockchain.Model[G]): EdgeNodeProgram[G] = new Implementation[G]

  def instance: EdgeNodeProgram[blockchain.Model.Op] = apply[blockchain.Model.Op]
}
