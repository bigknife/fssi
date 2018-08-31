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
        (Left(
          new FSSIException(
            s"payer's token($payerCurrentToken) is not enough to pay(${transfer.token})")): Either[
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

  def commit(block: Block): SP[F, Unit] = {
    import tokenStore._
    import contractDataStore._
    import contractStore._
    import blockStore._
    import log._

    val height = block.height
    for {
      _ <- saveBlock(block)
      _ <- cleanUndeterminedBlock(block)
      _ <- commitStagedToken(height)
      _ <- info(s"commit staged token of block($height)")
      _ <- commitStagedContract(height)
      _ <- info(s"commit staged contract of block($height)")
      _ <- commitStagedContractData(height)
      _ <- info(s"commit staged contract data of block($height)")
    } yield ()

  }

  def rollback(block: Block): SP[F, Unit] = {
    import tokenStore._
    import contractDataStore._
    import contractStore._
    import log._

    val height = block.height
    for {
      _ <- rollbackStagedToken(height)
      _ <- info(s"rollback staged token of block($height)")
      _ <- rollbackStagedContract(height)
      _ <- info(s"rollback staged contract of block($height)")
      _ <- rollbackStagedContractData(height)
      _ <- info(s"rollback staged contract data of block($height)")
    } yield ()
  }
}
