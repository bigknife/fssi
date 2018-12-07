package fssi.ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.types._
import fssi.types.biz.Message.ClientMessage.QueryTransaction
import fssi.types.biz._
import fssi.types.biz.Node._

@sp trait Network[F[_]] {
  def startupConsensusNode(handler: Message.Handler[ConsensusMessage, Unit]): P[F, ConsensusNode]
  def startupApplicationNode(
      handler: Message.Handler[ApplicationMessage, Unit]): P[F, ApplicationNode]
  def startupServiceNode(handler: Message.Handler[ClientMessage, Transaction]): P[F, ServiceNode]

  def shutdownConsensusNode(node: ConsensusNode): P[F, Unit]
  def shutdownApplicationNode(node: ApplicationNode): P[F, Unit]
  def shutdownServiceNode(node: ServiceNode): P[F, Unit]
  def broadcastMessage(message: Message): P[F, Unit]
  def handledQueryTransaction(queryTransaction: QueryTransaction): P[F, Option[Transaction]]
  def receiveTransactionMessage(applicationMessage: ApplicationMessage): P[F, Unit]
}
