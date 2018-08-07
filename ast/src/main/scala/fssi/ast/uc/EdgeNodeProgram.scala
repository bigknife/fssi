package fssi
package ast
package uc

import types._
import types.syntax._
import bigknife.sop._
import bigknife.sop.implicits._

trait EdgeNodeProgram[F[_]] {
  val model: components.Model[F]
  import model._

  /** Start up a edge node.
    * @return node info
    */
  def startup(handler: JsonMessageHandler): SP[F, Node] = for {
    n1 <- network.startupP2PNode(handler)
    n2 <- network.bindAccount(n1)
    //TODO: some other components should be initialized here
  } yield n2

  def broadcastMessage(message: JsonMessage): SP[F, Unit] = for {
    _ <- network.broadcastMessage(message)
  } yield ()

  /** Shutdown edge node
    */
  def shutdown(node: Node): SP[F, Unit] = for {
    _ <- network.shutdownP2PNode(node)
  } yield()
}
object EdgeNodeProgram {
  def apply[F[_]](implicit M: components.Model[F]): EdgeNodeProgram[F] = new EdgeNodeProgram[F] {
    val model: components.Model[F] = M
  }
}
