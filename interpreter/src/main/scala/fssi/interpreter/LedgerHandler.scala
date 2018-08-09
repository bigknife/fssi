package fssi
package interpreter

import types._, implicits._
import ast._

import scala.collection._

class LedgerHandler extends Ledger.Handler[Stack] {
  override def createGenesisBlock(chainID: String): Stack[Block] = Stack {
    val b1 = Block(
      hash = Hash.empty,
      previousHash = Hash.empty,
      height = 0,
      transactions = immutable.TreeSet.empty[Transaction],
      chainID = chainID
    )
    hashBlock(b1)
  }

  /** calculate the hash(sha3-256) of a block
    * then, fill the `hash` field to return the new block.
    * the participating fileds include previousHash, height, transactions and chainID
    */
  private def hashBlock(block: Block): Block = {
    val allBytes = block.previousHash.bytes ++ block.height.toByteArray ++ block.transactions
      .foldLeft(Array.emptyByteArray)((acc, n) => acc ++ bytesToHashForTransaction(n)) ++ block.chainID
      .getBytes("utf-8")
    val hashBytes = cryptoUtil.hash(allBytes)
    block.copy(hash = Hash(hashBytes))
  }

  /** serialize transaction to byte array.
    * we should guarantee the serialization is determined,
    * for the same transaction, we should get the same byte array.
    */
  private def bytesToHashForTransaction(transaction: Transaction): Array[Byte] = transaction match {
    case x: Transaction.Transfer =>
      x.id.value.getBytes("utf-8") ++
        x.from.value.bytes ++
        x.to.value.bytes ++
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
        case PString(x)     => x.getBytes("utf-8")
        case PBigDecimal(x) => x.toString.getBytes("")
        case PBool(x)       => BigInt(if (x) 1 else 0).toByteArray
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

}

object LedgerHandler {
  private val instance = new LedgerHandler

  trait Implicits {
    implicit val ledgerHandlerInstance: LedgerHandler = instance
  }

}
