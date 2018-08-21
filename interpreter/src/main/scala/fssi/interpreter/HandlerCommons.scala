package fssi
package interpreter

import utils._
import types._, implicits._
import ast._

import scala.collection._

trait HandlerCommons {

  /** calculate the hash(sha3-256) of a block
    * then, fill the `hash` field to return the new block.
    * the participating fileds include previousHash, height, transactions and chainID
    */
  private[interpreter] def hashBlock(block: Block): Block = {
    val allBytes = block.previousHash.bytes ++ block.height.toByteArray ++ block.transactions
      .foldLeft(Array.emptyByteArray)((acc, n) => acc ++ bytesToHashForTransaction(n)) ++ block.chainID
      .getBytes("utf-8")
    val hashBytes = cryptoUtil.hash(allBytes)
    block.copy(hash = Hash(hashBytes))
  }

  private[interpreter] def calculateTotalBlockBytes(block: Block): Array[Byte] = {
    val allBytes = block.previousHash.bytes ++
      block.height.toByteArray ++
      block.transactions
        .foldLeft(Array.emptyByteArray)((acc, n) => acc ++ bytesToHashForTransaction(n)) ++
      block.chainID.getBytes("utf-8") ++
      block.hash.bytes

    allBytes
  }

  /** serialize transaction to byte array.
    * we should guarantee the serialization is determined,
    * for the same transaction, we should get the same byte array.
    */
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
