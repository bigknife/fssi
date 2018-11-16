package fssi.ast.uc
package corenode

import bigknife.sop._
import bigknife.sop.implicits._
import fssi.types.biz.Message.ApplicationMessage.{QueryMessage, TransactionMessage}
import fssi.types.exception.FSSIException
import fssi.types.{ApplicationMessage, ConsensusMessage}
import fssi.types.implicits._

trait ProcessMessageProgram[F[_]]
    extends CoreNodeProgram[F]
    with BaseProgram[F]
    with HandleTransactionProgram[F] {
  import model._

  def processConsensusMessage(consensusMessage: ConsensusMessage): SP[F, Unit] = {
    for {
      lastDeterminedBlock <- store.getLatestDeterminedBlock()
      _                   <- consensus.processMessage(consensusMessage, lastDeterminedBlock)
    } yield ()
  }

  def processApplicationMessage(applicationMessage: ApplicationMessage): SP[F, Unit] = {
    applicationMessage match {
      case transactionMessage: TransactionMessage => processTransactionMessage(transactionMessage)
      case queryMessage: QueryMessage             => processQueryMessage(queryMessage)
    }
  }

  def broadcastConsensusMessage(consensusMessage: ConsensusMessage): SP[F, Unit] = {
    for {
      _ <- network.broadcastMessage(consensusMessage)
    } yield ()
  }

  private def processTransactionMessage(transactionMessage: TransactionMessage): SP[F, Unit] = {
    for {
      transactionOpt <- contract.transferMessageToTransaction(transactionMessage)
      _ <- requireM(
        transactionOpt.isDefined,
        new FSSIException(
          s"transaction message ${transactionMessage.payload.asBytesValue.utf8String} can not transfer to transaction")
      )
      _ <- handleTransaction(transactionOpt.get)
    } yield ()
  }

  private def processQueryMessage(queryMessage: QueryMessage): SP[F, Unit] = ???

}
