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
    import tokenStore._
    for {
      payerCurrentToken <- getCurrentToken(transfer.payer)
      payeeCurrentToken <- getCurrentToken(transfer.payee)
      r <- if ((payerCurrentToken - transfer.token).amount >= 0)
        for {
          _ <- stageToken(height, transfer.payer, payerCurrentToken - transfer.token)
          _ <- stageToken(height, transfer.payee, payeeCurrentToken + transfer.token)
        } yield Right(()): Either[Throwable, Unit]
      else
        (Left(new FSSIException(
          s"payer's token(${payerCurrentToken}) is not enough to pay(${transfer.token})")): Either[
          Throwable,
          Unit]).pureSP[F]
    } yield r
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
