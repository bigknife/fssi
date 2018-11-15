package fssi.ast
package uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.types.base._
import fssi.types.biz._

import fssi.types.biz.Transaction._
import fssi.types.implicits._

trait HandleTransactionProgram[F[_]] extends CoreNodeProgram[F] with BaseProgram[F] {
  import model._

  def handleTransaction(transaction: Transaction): SP[F, Receipt] = {
    for {
      verifyResult      <- crypto.verifyTransactionSignature(transaction)
      _                 <- requireM(verifyResult(), new RuntimeException("transaction signature tampered"))
      receipt           <- runTransaction(transaction)
      determinedBlock   <- store.getLatestDeterminedBlock()
      currentWorldState <- store.getCurrentWorldState()
      _                 <- consensus.tryAgree(transaction, receipt, determinedBlock, currentWorldState)
    } yield receipt
  }

  private def runTransaction(transaction: Transaction): SP[F, Receipt] = transaction match {
    case transfer: Transfer => runTransferTransaction(transfer)
    case deploy: Deploy     => runDeployTransaction(deploy)
    case run: Run           => runRunTransaction(run)
  }

  private def runTransferTransaction(transfer: Transfer): SP[F, Receipt] = {
    val message =
      s"payer ${transfer.payer.asBytesValue.bcBase58} transacts (${transfer.token}) to payee ${transfer.payee.asBytesValue.bcBase58}"
    for {
      _                  <- log.info(message)
      transactedOrFailed <- store.transactToken(transfer.payee, transfer.payer, transfer.token)
      receipt <- ifM(
        transactedOrFailed.isRight, {
          val info = s"$message success"
          for {
            _ <- log.info(info)
            log = Receipt.Log("INFO", info)
          } yield Receipt(transfer.id, success = true, Vector(log), 0)
        }
      )({
        val info = s"$message failed"
        for {
          _ <- log.error(info, Some(transactedOrFailed.left.get))
          log = Receipt.Log("ERROR", info)
        } yield Receipt(transfer.id, success = false, Vector(log), 0)
      })
    } yield receipt
  }

  private def runDeployTransaction(deploy: Deploy): SP[F, Receipt] = {
    for {
      contractName <- contract.getContractIdentifyName(deploy.contract)
      _ <- log.info(
        s"owner ${deploy.owner.asBytesValue.bcBase58} starts deploying contract $contractName")
      _ <- store.persistContract(contractName, deploy.contract)
      _ <- log.info(s"owner ${deploy.owner.asBytesValue.bcBase58} deployed contract $contractName")
    } yield Receipt(deploy.id, success = true, Vector.empty, 0)
  }

  private def runRunTransaction(run: Run): SP[F, Receipt] = ???
}
