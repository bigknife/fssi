package fssi.scp
package interpreter

import bigknife.sop._

import fssi.scp.ast._
import fssi.scp.types._

class NodeServiceHandler extends NodeService.Handler[Stack] {}

object NodeServiceHandler {
  val instance = new NodeServiceHandler

  trait Implicits {
    implicit val scpNodeServiceHandler: NodeServiceHandler = instance
  }
}
