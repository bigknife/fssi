package fssi
package interpreter

import utils._
import types._, implicits._
import ast._

import scala.collection._
import crypto.sha3_256digestInstance

class BlockServiceHandler extends BlockService.Handler[Stack] with BlockCalSupport {

  /** create an empty block
    */
    /*
  override def createFirstBlock(chainId: String): Stack[biz.Block] = Stack {
    biz.Block(
      head = biz.Block.Head(previousStates = biz.Block.emptyWordStates,
                            currentStates = biz.Block.emptyWordStates,
                            height = 0,
                            chainId = chainId),
      transactions = immutable.TreeSet.empty[biz.Transaction],
      hash = base.Hash.empty
    )
  }
  */

  /** update hash for the block
    */
  /*
  override def hashBlock(block: biz.Block): Stack[biz.Block] = Stack {
    // if hash is not empty, we will make it empty, and re-calclute the hash
    val b1: biz.Block =
      if (block.hash.asBytesValue.nonEmpty) block.copy(hash = base.Hash.empty) else block
    b1.copy(hash = base.Hash(b1.asBytesValue.digest.bytes))
  }
*/
  /*
  override def createGenesisBlock(chainID: String): Stack[Block] = Stack {
    val b1 = Block(
      hash = Hash.empty,
      previousHash = Hash.empty,
      previousTokenState = HexString.empty,
      previousContractState = HexString.empty,
      previousContractDataState = HexString.empty,
      height = 0,
      transactions = immutable.TreeSet.empty[Transaction],
      chainID = chainID
    )
    hashBlock(b1)
  }
  */

  /** check the hash of a block is corrent or not
    * @param block block to be verified, the hash should calclute correctly
    * @return if correct return true, or false.
    */
  /*
  override def verifyBlockHash(block: Block): Stack[Boolean] = Stack { setting =>
    val nb = hashBlock(block)
    nb.hash == block.hash
  }
  */

  /** serialize transaction to byte array.
    * we should guarantee the serialization is determined,
    * for the same transaction, we should get the same byte array.
    */
  /*
  private def bytesToHashForTransaction(transaction: Transaction): Array[Byte] = transaction match {
    case x: Transaction.Transfer =>
      x.id.value.getBytes("utf-8") ++
        x.payer.value.bytes ++
        x.payee.value.bytes ++
        x.token.amount.toByteArray ++ x.token.tokenUnit.toString.getBytes("utf-8") ++
        x.signature.value.bytes ++
        BigInt(x.timestamp).toByteArray

    case x: Transaction.PublishContract =>
      x.id.value.getBytes("utf-8") ++
        x.owner.value.bytes ++
        x.contract.owner.value.bytes ++
        x.contract.name.value.getBytes("utf-8") ++
        x.contract.version.value.getBytes("utf-8") ++
        x.contract.meta.methods.foldLeft(Array.emptyByteArray)((acc, n) =>
          acc ++ n.toString.getBytes("utf-8")) ++
        x.contract.signature.value.bytes ++
        x.signature.value.bytes ++
        BigInt(x.timestamp).toByteArray

    case x: Transaction.RunContract =>
      import Contract.Parameter._
      def bytesOfParam(p: Contract.Parameter): Array[Byte] = p match {
        case PEmpty          => Array.emptyByteArray
        case PString(x1)     => x1.getBytes("utf-8")
        case PBigDecimal(x1) => x1.toString.getBytes("")
        case PBool(x1)       => BigInt(if (x1) 1 else 0).toByteArray
        case PArray(array) =>
          array.foldLeft(Array.emptyByteArray)((acc, n) => acc ++ bytesOfParam(n))
      }

      x.id.value.getBytes("utf-8") ++
        x.sender.value.bytes ++
        x.contractName.value.getBytes("utf-8") ++
        x.contractVersion.value.getBytes("utf-8") ++
        bytesOfParam(x.contractParameter) ++
        x.signature.value.bytes ++
        BigInt(x.timestamp).toByteArray
  }
  */
}

object BlockServiceHandler {
  private val instance = new BlockServiceHandler

  trait Implicits {
    implicit val blockServiceHandlerInstance: BlockServiceHandler = instance
  }

}
