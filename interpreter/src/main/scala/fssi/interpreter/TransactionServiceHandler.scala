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
  override def calculateSingedBytesOfTransfer(transfer: Transaction.Transfer): Stack[BytesValue] =
    Stack { setting =>
      // id, payer, payee, token, timestamp
      BytesValue(transfer.id.value.getBytes("utf-8")) ++
        transfer.payer.value.toBytesValue ++ transfer.payee.value.toBytesValue ++
        BytesValue(transfer.token.toString.getBytes("utf-8")) ++
        BytesValue(BigInt(transfer.timestamp).toByteArray)
    }

  /** calculate bytes of the PublishContract object which will be signed
    */
  override def calculateSingedBytesOfPublishContract(
      publishContract: Transaction.PublishContract): Stack[BytesValue] = Stack { setting =>
    BytesValue(publishContract.id.value.getBytes("utf-8")) ++
      publishContract.owner.value.toBytesValue ++
      publishContract.contract.owner.value.toBytesValue ++
      BytesValue(publishContract.contract.name.value.getBytes("utf-8")) ++
      BytesValue(publishContract.contract.version.value.getBytes("utf-8")) ++
      publishContract.contract.code.toBytesValue ++
      publishContract.contract.meta.methods.foldLeft(BytesValue.empty)((acc, n) =>
        acc ++ BytesValue(n.toString.getBytes("utf-8"))) ++
      publishContract.contract.signature.value.toBytesValue ++
      BytesValue(BigInt(publishContract.timestamp).toByteArray)
  }

  /** calculate bytes of the RunContract object which will be signed
    */
  override def calculateSingedBytesOfRunContract(
      runContract: Transaction.RunContract): Stack[BytesValue] = Stack { setting =>
    import Contract.Parameter._
    def parametersToBytesValue(parameter: Contract.Parameter): BytesValue = parameter match {
      case PString(value) => BytesValue(value.getBytes("utf-8"))
      case PBigDecimal(value) =>
        BytesValue(value.unscaledValue.toByteArray) ++ BytesValue(BigInt(value.scale).toByteArray)
      case PBool(value) => if (value) BytesValue(Array(1.toByte)) else BytesValue(Array(0.toByte))
      case PArray(array) =>
        array.foldLeft(BytesValue.empty)((acc, n) => acc ++ parametersToBytesValue(n))
    }

    BytesValue(runContract.id.value.getBytes("utf-8")) ++
      runContract.sender.value.toBytesValue ++
      BytesValue(runContract.contractName.value.getBytes("utf-8")) ++
      BytesValue(runContract.contractVersion.value.getBytes("utf-8")) ++
      BytesValue(runContract.contractMethod.toString.getBytes("utf-8")) ++
      parametersToBytesValue(runContract.contractParameter) ++
      BytesValue(BigInt(runContract.timestamp).toByteArray)
  }

}

object TransactionServiceHandler {
  private val instance = new TransactionServiceHandler

  trait Implicits {
    implicit val transactionServiceHandler: TransactionServiceHandler = instance
  }
}
