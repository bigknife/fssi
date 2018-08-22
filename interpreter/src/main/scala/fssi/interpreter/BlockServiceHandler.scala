package fssi
package interpreter

import utils._
import types._, implicits._
import ast._

import scala.collection._

class BlockServiceHandler extends BlockService.Handler[Stack] with HandlerCommons {
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

  /** check the hash of a block is corrent or not
    * @param block block to be verified, the hash should calclute correctly
    * @return if correct return true, or false.
    */
  override def verifyBlockHash(block: Block): Stack[Boolean] = Stack { setting =>
    val nb = hashBlock(block)
    nb.hash == block.hash
  }
}

object BlockServiceHandler {
  private val instance = new BlockServiceHandler

  trait Implicits {
    implicit val blockServiceHandlerInstance: BlockServiceHandler = instance
  }

}
