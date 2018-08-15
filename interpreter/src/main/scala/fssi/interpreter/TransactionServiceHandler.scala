package fssi
package interpreter

import types._
import utils._
import ast._

class TransactionServiceHandler extends TransactionService.Handler[Stack] {

  /** create a transfer object with an empty signature field
    */
  override def createUnsignedTransfer(payer: Account.ID,
                                      payee: Account.ID,
                                      token: Token): Stack[Transaction.Transfer] = Stack {
    setting =>
      val randomId = Transaction.ID(
        java.util.UUID.randomUUID.toString
      )
      Transaction.Transfer(
        randomId,
        payer,
        payee,
        token,
        Signature.empty,
        System.currentTimeMillis
      )
  }

  /** calculate bytes of the transfer object which will be signed
    */
  override def toBeSingedBytesOfTransfer(transfer: Transaction.Transfer): Stack[BytesValue] =
    Stack { setting =>
      // id, payer, payee, token, timestamp
      BytesValue(transfer.id.value.getBytes("utf-8")) ++
        transfer.payer.value.toBytesValue ++ transfer.payee.value.toBytesValue ++
        BytesValue(transfer.token.toString.getBytes("utf-8")) ++
        BytesValue(BigInt(transfer.timestamp).toByteArray)
    }
}

object TransactionServiceHandler {
  private val instance = new TransactionServiceHandler

  trait Implicits {
    implicit val transactionServiceHandler: TransactionServiceHandler = instance
  }
}
