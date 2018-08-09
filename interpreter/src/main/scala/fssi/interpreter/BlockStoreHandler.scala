package fssi
package interpreter

import jsonCodecs._
import io.circe._
import io.circe.syntax._

import utils._, trie._
import types._, implicits._
import ast._
import java.io._


class BlockStoreHandler extends BlockStore.Handler[Stack] {
  private val blockFileDirName = "block"
  private val blockTrie: Once[Trie] = Once.empty

  /** initialize a data directory to be a block store
    * @param dataDir directory to save block.
    */
  override def initialize(dataDir: File): Stack[Unit] = Stack {
    val path = new File(dataDir, blockFileDirName)
    path.mkdirs()
    blockTrie := Trie.empty(levelDBStore(path))
  }

  /** save block, before saving, invoker should guarantee that the block is legal
    * which means, block has correct hashes, correct height etc.
    * @param block a legal block.
    */
  override def saveBlock(block: Block): Stack[Unit] = Stack {
    // save block in blockTrie
    // block will saved as json
    // key is the height of the block
    blockTrie foreach {trie =>
      val key = block.height.toByteArray
      val value = block.asJson.noSpaces.getBytes("utf-8")
      trie.store.save(key, value)
    }
  }
}

object BlockStoreHandler {
  private val instance = new BlockStoreHandler()

  trait Implicits {
    implicit val blockStoreHandlerInstance: BlockStoreHandler = instance
  }
}
