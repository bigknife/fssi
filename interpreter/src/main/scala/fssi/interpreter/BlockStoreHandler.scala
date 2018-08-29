package fssi
package interpreter

import jsonCodecs._
import utils._
import trie._
import types._
import implicits._
import ast._
import java.io._
import java.nio.charset.Charset
import scala.collection._
import io.circe.parser._
import io.circe.syntax._
import Bytes.implicits._
import better.files.{File => ScalaFile, _}

class BlockStoreHandler extends BlockStore.Handler[Stack] with BlockCalSupport with LogSupport {
  private val blockFileDirName                               = "block"
  private val blockTrie: SafeVar[Trie[Char, String]]         = SafeVar.empty
  private val blockStore: Once[LevelDBStore[String, String]] = Once.empty
  private val blockTrieJsonFile: Once[ScalaFile]             = Once.empty

  // current block height key in block trie
  private val KEY_CURRENT_HEIGHT
    : Array[Char] = HexString("current_height".getBytes).noPrefix.toCharArray

  // current undetermined block
  private val undeterminedBlockRef: java.util.concurrent.atomic.AtomicReference[Option[Block]] =
    new java.util.concurrent.atomic.AtomicReference(None)

  /** initialize a data directory to be a block store
    * @param dataDir directory to save block.
    */
  override def initializeBlockStore(dataDir: File): Stack[Unit] = Stack { setting =>
    val path = new File(dataDir, blockFileDirName)
    path.mkdirs()
    blockTrieJsonFile := new File(path, "block.trie.json").toScala
    blockTrieJsonFile.foreach { f =>
      if (f.exists && f.isRegularFile) {
        //reload
        val reloadResult = for {
          json <- parse(f.contentAsString(Charset.forName("utf-8")))
          trie <- json.as[Trie[Char, String]]
        } yield trie

        reloadResult match {
          case Left(t) =>
            log.error("reload block trie faield", t)
            throw t
          case Right(trie) =>
            blockTrie := trie
            log.info(s"reloaded block trie, root hash = ${trie.rootHexHash}")
        }
      } else if (f.notExists) {
        //init
        val trie = Trie.empty[Char, String]
        f.overwrite(trie.asJson.spaces2)
        blockTrie := trie
        log.info("init block trie.")
      } else
        throw new RuntimeException(s"$f should be empty to init or a regular file to load.")

      // init or load level db store
      val dbFile = new File(path, "db")
      blockStore := levelDBStore(dbFile)
      log.info(s"init leveldb at $dbFile")
    }
  }

  /** save block, before saving, invoker should guarantee that the block is legal
    * which means, block has correct hashes, correct height etc.
    * @param block a legal block.
    */
  override def saveBlock(block: Block): Stack[Unit] = Stack { setting =>
    // save block in blockTrie
    // block will saved as json
    // key is the height of the block
    blockTrie.updated { trie =>
      // current height value is stored in trie, with referenced by KEY_CURRENT_HEIGHT
      val currentHeightValue = HexString(block.height.toByteArray).toString

      // the current height as a key to refer to a block hash
      val currentHeightKey = HexString(block.height.toByteArray).noPrefix.toCharArray

      // block hash value, referred by currentHeightKey
      // and also the key of block in the block store
      val blockHashValue = block.hash.value.toString

      // block content json stored in a store
      // refered by blockHashValue
      val storedValue = block.asJson.noSpaces

      // store value
      blockStore.foreach { store =>
        store.save(blockHashValue, storedValue)
        log.info(s"block(height=${block.height}) saved to db with key = $blockHashValue")
      }

      trie
        .put(KEY_CURRENT_HEIGHT, currentHeightValue)
        .unsafe {
          log.info(s"saved current height to block trie, $currentHeightValue")
        }
        .put(currentHeightKey, blockHashValue)
        .unsafe { trie =>
          log.info(s"saved blockHashValue = $blockHashValue with key = $currentHeightKey")
          // save trie to json file
          blockTrieJsonFile.foreach { f =>
            f.overwrite(trie.asJson.spaces2)
            log.info(s"saved block json file to $f")
          }
        }

    }
  }

  /** get latest saved block, which block must have been gone through consensus
    */
  override def getLatestDeterminedBlock(): Stack[Block] = Stack { setting =>
    blockTrie
      .map { trie =>
        val currentHeightKey =
          HexString.decode(trie.get(KEY_CURRENT_HEIGHT).get).noPrefix.toCharArray
        val blockHashValue: String = trie.get(currentHeightKey).get
        val blockContent: String   = blockStore.map(_.load(blockHashValue)).unsafe().get

        val blockEither = for {
          valueJson <- parse(blockContent)
          block     <- valueJson.as[Block]
        } yield block
        blockEither.right.get
      }
      .unsafe()
  }

  /** get current undetermined block, and append a transaction into it, which is on consensus
    * @param determinedBlock the latest determined block which current undetermined block is based on.
    */
  override def appendTransactionToUnDeterminedBlock(determinedBlock: Block,
                                                    transaction: Transaction): Stack[Block] =
    Stack { setting =>
      // if current undeterminded block is empty,
      // create a new block from last determined block, update the transaction
      // or insert the transaction
      val newBlock = if (undeterminedBlockRef.get.isEmpty) {
        determinedBlock.copy(
          previousHash = determinedBlock.hash,
          height = determinedBlock.height + 1,
          transactions = immutable.TreeSet(transaction)
        )
      } else {
        val block = undeterminedBlockRef.get.get
        block.copy(
          transactions = block.transactions + transaction
        )
      }
      val hashedBlock = hashBlock(newBlock)
      undeterminedBlockRef.set(Some(hashedBlock))
      hashedBlock

    }
}

object BlockStoreHandler {
  private val instance = new BlockStoreHandler()

  trait Implicits {
    implicit val blockStoreHandlerInstance: BlockStoreHandler = instance
  }
}
