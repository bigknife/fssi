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
}

object BlockServiceHandler {
  private val instance = new BlockServiceHandler

  trait Implicits {
    implicit val blockServiceHandlerInstance: BlockServiceHandler = instance
  }

}
