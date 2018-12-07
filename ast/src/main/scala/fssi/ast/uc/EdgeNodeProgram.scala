package fssi.ast
package uc
import fssi.types.biz.{Message, Transaction}
import bigknife.sop._
import bigknife.sop.implicits._
import fssi.ast.uc.edgenode.{ProcessMessageProgram, ShutdownProgram, StartupProgram}
import fssi.types.{ApplicationMessage, ClientMessage}
import fssi.types.biz.Node.{ApplicationNode, ServiceNode}

trait EdgeNodeProgram[F[_]] {

  /** start up a edge node
    * @param applicationMessageHandler handler to handle application query message
    * @param clientMessageHandler handler to handle client json rpc message
    */
  def startup(applicationMessageHandler: Message.Handler[ApplicationMessage, Unit],
              clientMessageHandler: Message.Handler[ClientMessage, Transaction])
    : SP[F, (ApplicationNode, ServiceNode)]

  def shutdown(applicationNode: ApplicationNode, serviceNode: ServiceNode): SP[F, Unit]

  def processApplicationMessage(applicationMessage: ApplicationMessage): SP[F, Unit]

  def processClientMessage(clientMessage: ClientMessage): SP[F, Transaction]
}

object EdgeNodeProgram {

  final class Implementation[G[_]](implicit M: blockchain.Model[G])
      extends EdgeNodeProgram[G]
      with StartupProgram[G]
      with ShutdownProgram[G]
      with ProcessMessageProgram[G] {
    override private[uc] val model = M
  }

  def apply[G[_]](implicit M: blockchain.Model[G]): EdgeNodeProgram[G] = new Implementation[G]

  def instance: EdgeNodeProgram[blockchain.Model.Op] = apply[blockchain.Model.Op]
}
