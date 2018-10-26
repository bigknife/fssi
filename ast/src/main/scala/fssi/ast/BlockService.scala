package fssi
package ast

import types._, biz._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

@sp trait BlockService[F[_]] {

  /** create an empty block
    */
  def createFirstBlock(chainId: String): P[F, biz.Block]

  /** update hash for the block
    */
  def hashBlock(block: biz.Block): P[F, biz.Block]

  /** create the genesis block for a chain
    * @param chainID the chain identity
    * @return genesis block
    */
  def createGenesisBlock(chainID: String): P[F, Block]

  /** check the hash of a block is corrent or not
    * @param block block to be verified, the hash should calclute correctly
    * @return if correct return true, or false.
    */
  def verifyBlockHash(block: Block): P[F, Boolean]
}
