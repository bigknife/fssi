package fssi.types

import scala.collection._

/** Block is a data structure for saving immutable data into blockchain.
  * @param hash the hash of current block, it can be used as a block's ID
  * @param previousHash the hash of preivous block.
  * @param height current block height
  * @param transactions included transactions, 
  *                     through all of these, the initial state reached to final state in this block.
  * @param chainID a simple string to identify a chain where the block was created and saved.
  */
case class Block(
  hash: Hash,
  previousHash: Hash,
  previousTokenState: HexString,
  previousContractState: HexString,
  previousContractDataState: HexString,
  height: BigInt,
  transactions: immutable.TreeSet[Transaction],
  chainID: String
)
