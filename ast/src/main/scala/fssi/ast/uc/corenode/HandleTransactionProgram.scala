package fssi.ast
package uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.types.base._
import fssi.types.biz.Contract.UserContract.Method
import fssi.types.biz._
import fssi.types.biz.Transaction._
import fssi.types.exception.FSSIException
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
      _                <- log.info(message)
      snapshotOrFailed <- store.snapshotTransaction(transfer)
      receipt <- ifM(
        snapshotOrFailed.isRight.pureSP[F], {
          val info = s"$message success"
          for {
            _ <- log.info(info)
            log = Receipt.Log("INFO", info)
          } yield Receipt(transfer.id, success = true, Vector(log), 0)
        }
      )({
        val info = s"$message failed"
        for {
          _ <- log.error(info, Some(snapshotOrFailed.left.get))
          log = Receipt.Log("ERROR", info)
        } yield Receipt(transfer.id, success = false, Vector(log), 0)
      })
    } yield receipt
  }

  private def runDeployTransaction(deploy: Deploy): SP[F, Receipt] = {
    val owner = deploy.owner.asBytesValue.bcBase58
    val name  = deploy.contract.name.asBytesValue.bcBase58
    for {
      _      <- log.info(s"owner $owner starts deploying contract $name")
      passed <- store.canDeployNewTransaction(deploy)
      r <- ifM(
        passed.pureSP[F], {
          for {
            _ <- store.snapshotTransaction(deploy)
            _ <- log.info(s"owner $owner deployed contract $name success")
          } yield Receipt(deploy.id, success = true, Vector.empty, 0)
        }
      ) {
        for {
          _ <- log.info(s"owner $owner deployed contract $name failed")
        } yield Receipt(deploy.id, success = false, Vector.empty, 0)
      }
    } yield r
  }

  private def runRunTransaction(run: Run): SP[F, Receipt] = {
    val caller          = run.caller.asBytesValue.bcBase58
    val owner           = run.owner.asBytesValue.bcBase58
    val contractName    = run.contractName.asBytesValue.bcBase58
    val contractVersion = run.contractVersion
    val info =
      s"caller $caller start running contract $contractName belonged to $owner at version $contractVersion"
    for {
      _ <- log.info(info)
      userContractCodeOpt <- store.loadContractCode(run.owner,
                                                    run.contractName,
                                                    run.contractVersion)
      _ <- requireM(
        userContractCodeOpt.nonEmpty,
        new FSSIException(
          s"contract $contractName at version $contractVersion published by $owner not found")
      )
      kvStore    <- store.prepareKVStore(run.caller, run.contractName, run.contractVersion)
      tokenQuery <- store.prepareTokenQuery()
      context    <- store.createContextInstance(kvStore, tokenQuery, run.caller)
      result <- contract.invokeContract(context,
                                        userContractCodeOpt.get,
                                        Method(run.methodAlias, ""),
                                        run.contractParameter)
      r <- ifM(
        result.isRight.pureSP[F], {
          for {
            _ <- store.snapshotTransaction(run)
            msg = s"caller $caller run contract $contractName belonged to $owner at version $contractVersion success"
            _ <- log.info(msg)
            logs = Vector(Receipt.Log(label = "INFO", info), Receipt.Log("INFO", msg))
          } yield Receipt(run.id, success = true, logs, 0)
        }
      ) {
        val error =
          s"caller $caller run contract $contractName belonged to $owner at version $contractVersion failed: ${result.left.get.getMessage}"
        for {
          _ <- log.error(error)
          logs = Vector(Receipt.Log("INFO", info), Receipt.Log("ERROR", error))
        } yield Receipt(run.id, success = false, logs, 0)
      }
    } yield r
  }
}
