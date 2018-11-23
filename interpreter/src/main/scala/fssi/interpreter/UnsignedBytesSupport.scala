package fssi.interpreter
import fssi.types.biz.Contract.UserContract
import fssi.types.biz.{Block, Transaction}
import fssi.types.implicits._

trait UnsignedBytesSupport {

  def calculateUnsignedBlockBytes(block: Block): Array[Byte] = {
    import block._
    height.asBytesValue.bytes ++ chainId.asBytesValue.bytes ++ preWorldState.asBytesValue.bytes ++ curWorldState.asBytesValue.bytes ++ transactions
      .foldLeft(Array.emptyByteArray)((acc, n) => acc ++ calculateUnsignedTransactionBytes(n)) ++ receipts.toArray.asBytesValue.bytes
  }

  def calculateUnsignedTransactionBytes(transaction: Transaction): Array[Byte] = {
    transaction match {
      case Transaction.Transfer(id, payer, publicKeyForVerifying, payee, token, _, _) =>
        id.asBytesValue.bytes ++ payer.asBytesValue.bytes ++ publicKeyForVerifying.asBytesValue.bytes ++ payee.asBytesValue.bytes ++ token.asBytesValue.bytes
      case Transaction.Deploy(id, owner, publicKeyForVerifying, contract, _, _) =>
        id.asBytesValue.bytes ++ owner.asBytesValue.bytes ++ publicKeyForVerifying.asBytesValue.bytes ++ calculateContractBytes(
          contract)
      case Transaction.Run(id,
                           caller,
                           publicKeyForVerifying,
                           contractName,
                           contractVersion,
                           methodAlias,
                           contractParameter,
                           _,
                           _) =>
        id.asBytesValue.bytes ++ caller.asBytesValue.bytes ++ publicKeyForVerifying.asBytesValue.bytes ++ contractName.asBytesValue.bytes ++ contractVersion.asBytesValue.bytes ++ methodAlias.asBytesValue.bytes ++ contractParameter.asBytesValue.bytes
    }
  }

  def calculateContractBytes(contract: UserContract): Array[Byte] = {
    import contract._
    owner.asBytesValue.bytes ++ name.asBytesValue.bytes ++ version.asBytesValue.bytes ++ code.asBytesValue.bytes ++ methods.toArray.asBytesValue.bytes
  }
}

object UnsignedBytesSupport extends UnsignedBytesSupport
