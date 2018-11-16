package fssi.ast.uc.edgenode
import bigknife.sop._
import bigknife.sop.implicits._
import fssi.ast.uc.{BaseProgram, EdgeNodeProgram}
import fssi.types.biz.Message.ClientMessage.{QueryTransaction, SendTransaction}
import fssi.types.biz.Transaction
import fssi.types.exception.FSSIException
import fssi.types.{ApplicationMessage, ClientMessage}
import fssi.types.implicits._

trait ProcessMessageProgram[F[_]] extends EdgeNodeProgram[F] with BaseProgram[F] {
  import model._

  def processApplicationMessage(applicationMessage: ApplicationMessage): SP[F, Unit] = {
    for {
      _ <- network.receiveTransactionMessage(applicationMessage)
    } yield ()
  }

  def processClientMessage(clientMessage: ClientMessage): SP[F, Transaction] = {
    clientMessage match {
      case sendTransaction: SendTransaction   => processSendTransactionMessage(sendTransaction)
      case queryTransaction: QueryTransaction => processQueryTransactionMessage(queryTransaction)
    }
  }

  private def processSendTransactionMessage(
      sendTransaction: SendTransaction): SP[F, Transaction] = {
    for {
      transactionOpt <- contract.transferMessageToTransaction(sendTransaction)
      _ <- requireM(
        transactionOpt.isDefined,
        new FSSIException(
          s"send transaction message ${sendTransaction.payload.asBytesValue.utf8String} can not transfer to Transaction")
      )
      _ <- network.broadcastMessage(sendTransaction)
    } yield transactionOpt.get
  }

  private def processQueryTransactionMessage(
      queryTransaction: QueryTransaction): SP[F, Transaction] = {
    for {
      transactionOpt <- network.handledQueryTransaction(queryTransaction)
      transaction <- ifM(transactionOpt.isDefined, transactionOpt.get)(
        sendQueryTransactionToCoreNode(queryTransaction))
    } yield transaction
  }

  private def sendQueryTransactionToCoreNode(
      queryTransaction: QueryTransaction): SP[F, Transaction] = ???
}
