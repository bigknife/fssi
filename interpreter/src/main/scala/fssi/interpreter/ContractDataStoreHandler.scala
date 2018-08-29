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

class ContractDataStoreHandler extends ContractDataStore.Handler[Stack] with LogSupport {
  private val contractDataFileName         = "contractdata"
  private val contractDataTrie: SafeVar[Trie[Char, String]] = SafeVar.empty
  private val contractDataStore: Once[LevelDBStore[String, String]] = Once.empty
  private val contractDataTrieJsonFile: Once[ScalaFile] = Once.empty


  override def initializeContractDataStore(dataDir: File): Stack[Unit] = Stack {setting =>
    val path = new File(dataDir, contractDataFileName)
    path.mkdirs()
    contractDataTrieJsonFile := new File(path, "contract.trie.json").toScala
    contractDataTrieJsonFile.foreach { f =>
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
            contractDataTrie := trie
            log.info(s"reloaded block trie, root hash = ${trie.rootHexHash}")
        }
      } else if (f.notExists) {
        //init
        val trie = Trie.empty[Char, String]
        f.overwrite(trie.asJson.spaces2)
        contractDataTrie := trie
        log.info("init block trie.")
      } else
        throw new RuntimeException(s"$f should be empty to init or a regular file to load.")

      // init or load level db store
      val dbFile = new File(path, "db")
      dbFile.mkdirs()
      contractDataStore := levelDBStore(dbFile)
      log.info(s"init leveldb at $dbFile")
    }
  }

  /** self test for a contract data store
    * @param block contract data store should be tested on block
    * @return if the store is sane return true, or false
    */
  override def testContractDataStore(block: Block): Stack[Boolean] = Stack { setting =>
    // if can be reloaded, it's always true
    true
  }

  /** get current contract data store state
    * this state should identify current state of contract data store
    */
  override def getContractDataStoreState(): Stack[HexString] = Stack { setting =>
    contractDataTrie.map { _.rootHexHash }.map(HexString.decode).unsafe()
  }

  /** verify current state of contract data store
    */
  override def verifyContractDataStoreState(state: String): Stack[Boolean] = Stack { setting =>
    true
  }
}

object ContractDataStoreHandler {
  private val instance = new ContractDataStoreHandler()

  trait Implicits {
    implicit val contractDataStoreHandlerInstance: ContractDataStoreHandler = instance
  }
}
