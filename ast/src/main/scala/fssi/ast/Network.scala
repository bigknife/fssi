package fssi.ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.biz._

@sp trait Network[F[_]] {
  def startupPeerNode(conf: ChainConfiguration, handler: JsonMessageHandler): P[F, Node]
  def shutdown(): P[F, Unit]
}
