package fssi.ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.biz._
import fssi.types.biz.Node._

@sp trait Consensus[F[_]] {
  def initialize(node: ConsensusNode):P[F, Unit]
  def destroy(): P[F, Unit]
  def tryAgree(transaction: Transaction, receipt: Receipt): P[F, Unit]
  def processMessage(message: ConsensusAuxMessage): P[F, Unit]
}
