package fssi.ast.uc
package edgenode
import bigknife.sop._
import bigknife.sop.implicits._
import fssi.types.biz.{Message, Transaction}
import fssi.types.biz.Message.{ApplicationMessage, ClientMessage}
import fssi.types.biz.Node.{ApplicationNode, ServiceNode}

trait StartupProgram[F[_]] extends EdgeNodeProgram[F] with BaseProgram[F] {

  import model._

  /** start up a edge node
    * @param applicationMessageHandler handler to handle application query message
    * @param clientMessageHandler handler to handle client json rpc message
    */
  def startup(applicationMessageHandler: Message.Handler[ApplicationMessage, Unit],
              clientMessageHandler: Message.Handler[ClientMessage, Transaction])
    : SP[F, (ApplicationNode, ServiceNode)] = {
    for {
      applicationNode <- network.startupApplicationNode(applicationMessageHandler)
      _               <- log.info("application node start up")
      serviceNode     <- network.startupServiceNode(clientMessageHandler)
      _               <- log.info("service node start up")
    } yield (applicationNode, serviceNode)
  }
}
