package fssi.ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.biz._

@sp trait Contract[F[_]] {
  /** check runtime, if not acceptable, throw exception
    */
  def assertRuntime(): P[F, Unit]
  def initializeRuntime(): P[F, Unit]
  def closeRuntime(): P[F, Unit]
  def runTransaction(transaction: Transaction): P[F, Receipt]
}
