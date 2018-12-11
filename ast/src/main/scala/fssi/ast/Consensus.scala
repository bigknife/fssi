package fssi.ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.types.{ConsensusMessage, TransactionSet}
import fssi.types.biz._
import fssi.types.biz.Node._

@sp trait Consensus[F[_]] {
  def initialize(node: ConsensusNode, currentHeight: BigInt): P[F, Unit]
  def destroy(): P[F, Unit]
  def isConsensusLasting(): P[F, Boolean]
  def startConsensus(): P[F, Unit]
  def stopConsensus(): P[F, Unit]
  def processMessage(message: ConsensusMessage, lastDeterminedBlock: Block): P[F, Unit]
  def agreeTransactions(transactions: TransactionSet): P[F, Unit]
  def prepareExecuteAgreeProgram(program: Any): P[F, Unit]
}
