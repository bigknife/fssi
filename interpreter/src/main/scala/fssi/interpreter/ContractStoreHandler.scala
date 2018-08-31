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

import java.io._

class ContractStoreHandler extends ContractStore.Handler[Stack] with LogSupport {
  private val contractFileDirName = "contract"

  private val contractTrie: SafeVar[Trie[Char, String]]         = SafeVar.empty
  private val contractStore: Once[LevelDBStore[String, String]] = Once.empty
  private val contractTrieJsonFile: Once[ScalaFile]             = Once.empty

  /** initialize a data directory to be a contract store
    * @param dataDir directory to save contract.
    */
  override def initializeContractStore(dataDir: File): Stack[Unit] = Stack {
    val path = new File(dataDir, contractFileDirName)
    path.mkdirs()
    contractTrieJsonFile := new File(path, "contractdata.trie.json").toScala
    contractTrieJsonFile.foreach { f =>
      if (f.exists && f.isRegularFile) {
        //reload
        val reloadResult = for {
          json <- parse(f.contentAsString(Charset.forName("utf-8")))
          trie <- json.as[Trie[Char, String]]
        } yield trie

        reloadResult match {
          case Left(t) =>
            log.error("reload contract trie faield", t)
            throw t
          case Right(trie) =>
            contractTrie := trie
            log.info(s"reloaded contract trie, root hash = ${trie.rootHexHash}")
        }
      } else if (f.notExists) {
        //init
        val trie = Trie.empty[Char, String]
        f.overwrite(trie.asJson.spaces2)
        contractTrie := trie
        log.info("init contract trie.")
      } else
        throw new RuntimeException(s"$f should be empty to init or a regular file to load.")

      // init or load level db store
      val dbFile = new File(path, "db")
      dbFile.mkdirs()
      contractStore := levelDBStore(dbFile)
      log.info(s"init leveldb at $dbFile")
    }
  }

  /** self test for a contract store
    * @param block contract store should be tested on block
    * @return if the store is sane return true, or false
    */
  override def testContractStore(block: Block): Stack[Boolean] = Stack { setting =>
    true
  }

  /** get current token store state
    * this state should identify current state of token store
    */
  override def getTokenStoreState(): Stack[HexString] = Stack { setting =>
    contractTrie.map { _.rootHexHash }.map(HexString.decode).unsafe()
  }

  /** verify current state of contract store
    */
  override def verifyContractStoreState(state: String): Stack[Boolean] = Stack { setting =>
    true
  }

  /** commit staged tokens
    */
  override def commitStagedContract(height: BigInt): Stack[Unit] = Stack { setting =>
    }

  /** rollback staged tokens
    */
  override def rollbackStagedContract(height: BigInt): Stack[Unit] = Stack { setting =>
    }

}

object ContractStoreHandler {
  private val instance = new ContractStoreHandler()

  trait Implicits {
    implicit val contractStoreHandlerInstance: ContractStoreHandler = instance
  }
}
