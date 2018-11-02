package fssi.ast.uc
package edgenode
import fssi.types.biz.Node.{ApplicationNode, ServiceNode}
import bigknife.sop._
import bigknife.sop.implicits._

trait ShutdownProgram[F[_]] extends EdgeNodeProgram[F] with BaseProgram[F] {
  import model._

  override def shutdown(applicationNode: ApplicationNode, serviceNode: ServiceNode): SP[F, Unit] = {
    for {
      _ <- network.shutdownApplicationNode(applicationNode)
      _ <- network.shutdownServiceNode(serviceNode)
    } yield ()
  }
}
