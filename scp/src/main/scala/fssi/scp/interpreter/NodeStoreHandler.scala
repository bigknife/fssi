package fssi.scp
package interpreter

import bigknife.sop._

import fssi.scp.ast._
import fssi.scp.types._

class NodeStoreHandler extends NodeStore.Handler[Stack] {

}

object NodeStoreHandler {
  val instance = new NodeStoreHandler

  trait Implicits {
    implicit val scpNodeStoreHandler: NodeStoreHandler = instance
  }
}
