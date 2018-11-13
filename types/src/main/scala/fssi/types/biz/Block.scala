package fssi.types
package biz

import fssi.base.BytesValue
import fssi.types.base._
import fssi.types.implicits._

/** block contains a head and some transactions
  */
case class Block(
    head: Block.Head,
    transactions: TransactionSet,
    receipts: Set[Receipt],
    hash: Hash
)

object Block {

  /** block head contains state of the previous block and current block
    * and additional: current block height and chainID
    */
  case class Head(
      previousStates: HashState,
      currentStates: HashState,
      previousBlockHash: HashState,
      currentBlockHash: HashState,
      previousReceiptHash: HashState,
      currentReceiptHash: HashState,
      height: BigInt,
      chainId: String,
      timestamp: Timestamp
  )

  trait Implicits {
    implicit def blockHeadToBytesValue(a: Head): Array[Byte] = {
      import a._
      (previousStates.asBytesValue.any ++
        currentStates.asBytesValue.any ++
        previousBlockHash.asBytesValue.any ++
        currentBlockHash.asBytesValue.any ++
        previousReceiptHash.asBytesValue.any ++
        currentReceiptHash.asBytesValue.any ++
        height.asBytesValue.any ++
        chainId.asBytesValue.any).bytes
    }

    implicit def blockToBytesValue(a: Block): Array[Byte] = {
      import a._
      (head.asBytesValue.any ++ transactions
        .foldLeft(BytesValue.empty[Transaction])((acc, n) => acc ++ n.asBytesValue)
        .any ++ hash.asBytesValue.any).bytes
    }
  }

}
