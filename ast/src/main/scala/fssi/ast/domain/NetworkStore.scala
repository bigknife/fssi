package fssi.ast.domain

import bigknife.sop._,macros._,implicits._
import fssi.ast.domain.types._

@sp trait NetworkStore[F[_]] {
  /**
    * load current node
    * @return
    */
  def currentNode(): P[F, Option[Node]]

  /**
    * save node info locally
    * @param node node
    * @return
    */
  def saveNode(node: Node): P[F, Node]
}
