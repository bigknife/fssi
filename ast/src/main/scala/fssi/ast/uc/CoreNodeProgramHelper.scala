package fssi
package ast
package uc

import utils._
import types._, biz._
import types.implicits._
import bigknife.sop._
import bigknife.sop.implicits._
import Ordered._
import scala.collection._
import contract.lib._

private[uc] trait CoreNodeProgramHelper[F[_]] extends BaseProgram[F] {
  import model._

  /** run transfer
    * @return transaction account pair, first is payer, second is payee
    */
  def tempRunTransfer(height: BigInt,
    transfer: Transaction.Transfer): SP[F, Either[Throwable, Unit]] = {
    /*
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
     */
    ???
  }

  /** run publish contract
    */
  def tempRunPublishContract(
      height: BigInt,
      publishContract: Transaction.Deploy): SP[F, Either[Throwable, Unit]] = {
    // get the owner's token to check if he can afford to publish this contract
    // if he can afford, save user contract to a temp store reletived to height
    //
    /*
    import tokenStore._
    import contractService._
    import contractStore._

    for {
      publisherBalance <- getCurrentToken(publishContract.owner)
      cost             <- measureCostToPublishContract(publishContract)
      result <- if (publisherBalance < cost)
        Left(new FSSIException("publisher can't afford to publish a contract")).pureSP[F]
      else
        for {
          gid <- getContractGlobalIdentifiedName(publishContract.contract)
          _   <- stageContract(height, gid, publishContract.contract)
        } yield Right(())
    } yield result
     */
    ???
  }

  /** run run contract
    */
  def tempRunRunContract(height: BigInt,
                         runContract: Transaction.Run): SP[F, Either[Throwable, Unit]] = {
    import contractService._
    import contractStore._
    import contractDataStore._
    import tokenStore._

    /*
    for {
      contractOpt <- findUserContract(runContract.contractName, runContract.contractVersion)
      x <- if (contractOpt.isEmpty)
        Left(new FSSIException(
          s"UserContract(${runContract.contractName.value}#${runContract.contractVersion.value}) not found"))
          .pureSP[F]
      else
        for {
          sqlStore   <- prepareSqlStoreFor(height, contractOpt.get)
          kvStore    <- prepareKeyValueStoreFor(height, contractOpt.get)
          tokenQuery <- prepareTokenQueryFor(height, contractOpt.get)
          context <- createContextInstance(sqlStore,
                                           kvStore,
                                           tokenQuery,
                                           runContract.sender.value.toString)
          result <- invokeUserContract(context,
                                       contractOpt.get,
                                       runContract.contractMethod,
                                       runContract.contractParameter)
          _ <- closeSqlStore(sqlStore)
          _ <- closeKeyValueStore(kvStore)
        } yield result
    } yield x
     */
    ???
  }

  def commit(block: Block): SP[F, Unit] = {
    import tokenStore._
    import contractDataStore._
    import contractStore._
    import blockStore._
    import log._

    /*
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
     */
    ???
  }

  def rollback(block: Block): SP[F, Unit] = {
    import tokenStore._
    import contractDataStore._
    import contractStore._
    import log._

    /*
    val height = block.height
    for {
      _ <- rollbackStagedToken(height)
      _ <- info(s"rollback staged token of block($height)")
      _ <- rollbackStagedContract(height)
      _ <- info(s"rollback staged contract of block($height)")
      _ <- rollbackStagedContractData(height)
      _ <- info(s"rollback staged contract data of block($height)")
    } yield ()
     */
    ???
  }
}
