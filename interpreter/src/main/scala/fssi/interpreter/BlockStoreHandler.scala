package fssi
package interpreter

import types._, implicits._
import ast._
import java.io._

class BlockStoreHandler extends BlockStore.Handler[Stack] {

  /** initialize a data directory to be a block store
    * @param dataDir directory to save block.
    */
  override def initialize(dataDir: File): Stack[Unit] = Stack {
    dataDir.mkdirs()

    ???
  }

  /** save block, before saving, invoker should guarantee that the block is legal
    * which means, block has correct hashes, correct height etc.
    * @param block a legal block.
    */
  override def saveBlock(block: Block): Stack[Block] = Stack {
    ???
  }
}

object BlockStoreHandler {
  private val instance = new BlockStoreHandler()

  trait Implicits {
    implicit val blockStoreHandlerInstance: BlockStoreHandler = instance
  }
}
