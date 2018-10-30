package fssi
package interpreter
import fssi.ast.Network

class NetworkHandler extends Network.Handler[Stack] {}

object NetworkHandler {
  val instance = new NetworkHandler

  trait Implicits {
    implicit val networkHandler: NetworkHandler = instance
  }
}
