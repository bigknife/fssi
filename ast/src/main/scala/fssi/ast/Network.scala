package fssi.ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types._
import fssi.types.biz._
import fssi.types.biz.Node._

@sp trait Network[F[_]] {
  def startupConsensusNode(conf: ChainConfiguration,
                           handler: Message.Handler[ConsensusMessage]): P[F, ConsensusNode]
  def startupApplicationNode(conf: ChainConfiguration,
                             handler: Message.Handler[ApplicationMessage]): P[F, ApplicationNode]
  def startupServiceNode(conf: ChainConfiguration,
                         handler: Message.Handler[ClientMessage],
                         serviceResource: ServiceResource): P[F, ServiceNode]

  def shutdownConsensusNode(node: ConsensusNode): P[F, Unit]
  def shutdownApplicationNode(node: ApplicationNode): P[F, Unit]
  def shutdownServiceNode(node: ServiceNode): P[F, Unit]
}
