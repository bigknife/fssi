package fssi
package ast
package uc

import utils._
import types._, exception._
import types.syntax._
import bigknife.sop._
import bigknife.sop.implicits._
import scala.collection._

private[uc] trait CoreNodeProgramHelper[F[_]] extends BaseProgram[F] {
  import model._

  /** run transfer
    * @return transaction account pair, first is payer, second is payee
    */
  def tempRunTransfer(height: BigInt,
    transfer: Transaction.Transfer): SP[F, Either[Throwable, Unit]] = {

    ???
    
  }

  /** run publish contract
    */
  def tempRunPublishContract(
      height: BigInt,
      publishContract: Transaction.PublishContract): SP[F, Either[Throwable, Unit]] = ???

  /** run run contract
    */
  def tempRunRunContract(height: BigInt,
                         runContract: Transaction.RunContract): SP[F, Either[Throwable, Unit]] = ???

  def commit(height: BigInt): SP[F, Unit]   = ???
  def rollback(height: BigInt): SP[F, Unit] = ???
}
