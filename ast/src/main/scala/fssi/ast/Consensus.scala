package fssi.ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.types.ConsensusMessage
import fssi.types.base.WorldState
import fssi.types.biz._
import fssi.types.biz.Node._

@sp trait Consensus[F[_]] {
  def initialize(node: ConsensusNode, currentHeight: BigInt): P[F, Unit]
  def destroy(): P[F, Unit]
  def tryAgree(transaction: Transaction, lastDeterminedBlock: Block): P[F, Unit]
  def processMessage(message: ConsensusMessage, lastDeterminedBlock: Block): P[F, Unit]
}
