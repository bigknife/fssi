package fssi.types
package biz

import fssi.base.BytesValue
import fssi.types.base._
import fssi.types.implicits._

/** block contains a head and some transactions
  * @param height height or slotIndex of current block 
  * @param chainId chain id
  * @param preWorldState the block, transaction and receipt state of the block before this block
  * @param curWorldState the global state of the block before this block
  */
case class Block(
    height: BigInt,
    chainId: String,
    preWorldState: WorldState,
    curWorldState: WorldState,
    transactions: TransactionSet,
    receipts: ReceiptSet,
    timestamp: Timestamp,
    hash: Hash
)

object Block {

  trait Implicits {

    implicit def blockToBytesValue(a: Block): Array[Byte] = {
      import a._
      height.toByteArray ++
        chainId.getBytes("utf-8") ++
        (
          preWorldState.asBytesValue.any ++
            curWorldState.asBytesValue.any ++
            transactions
              .foldLeft(BytesValue.empty[Transaction])((acc, n) => acc ++ n.asBytesValue)
              .any ++
            timestamp.asBytesValue.any ++
            receipts.foldLeft(BytesValue.empty[Receipt])((acc, n) => acc ++ n.asBytesValue).any ++
            hash.asBytesValue.any
        ).bytes

    }
  }

}
