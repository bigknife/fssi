package fssi.interpreter
import fssi.types.biz.Contract.UserContract
import fssi.types.biz.{Block, Transaction}
import fssi.types.implicits._

trait UnsignedBytesSupport {

  protected def calculateUnsignedBlockBytes(block: Block): Array[Byte] = {
    import block._
    height.asBytesValue.bytes ++ chainId.asBytesValue.bytes ++ preWorldState.asBytesValue.bytes ++ curWorldState.asBytesValue.bytes ++ transactions.toArray.asBytesValue.bytes ++ receipts.toArray.asBytesValue.bytes ++ timestamp.asBytesValue.bytes ++ hash.asBytesValue.bytes
  }

  protected def calculateUnsignedTransactionBytes(transaction: Transaction): Array[Byte] = {
    transaction match {
      case Transaction.Transfer(id, payer, payee, token, _, timestamp, publicKeyForVerifying) =>
        id.asBytesValue.bytes ++ payer.asBytesValue.bytes ++ publicKeyForVerifying.asBytesValue.bytes ++ payee.asBytesValue.bytes ++ token.asBytesValue.bytes ++ timestamp.asBytesValue.bytes
      case Transaction.Deploy(id, owner, contract, _, timestamp, publicKeyForVerifying) =>
        id.asBytesValue.bytes ++ owner.asBytesValue.bytes ++ publicKeyForVerifying.asBytesValue.bytes ++ contract.asBytesValue.bytes ++ timestamp.asBytesValue.bytes
      case Transaction.Run(id,
                           caller,
                           contractName,
                           contractVersion,
                           methodAlias,
                           contractParameter,
                           _,
                           timestamp,
                           publicKeyForVerifying) =>
        id.asBytesValue.bytes ++ caller.asBytesValue.bytes ++ publicKeyForVerifying.asBytesValue.bytes ++ contractName.asBytesValue.bytes ++ contractVersion.asBytesValue.bytes ++ methodAlias.asBytesValue.bytes ++ contractParameter.asBytesValue.bytes ++ timestamp.asBytesValue.bytes
    }
  }

  protected def calculateContractBytes(contract: UserContract): Array[Byte] = {
    import contract._
    owner.asBytesValue.bytes ++ name.asBytesValue.bytes ++ version.asBytesValue.bytes ++ code.asBytesValue.bytes ++ methods.toArray.asBytesValue.bytes
  }
}
