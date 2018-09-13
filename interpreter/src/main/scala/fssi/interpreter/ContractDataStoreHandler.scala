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
import contract.lib._
import Bytes.implicits._
import better.files.{File => ScalaFile, _}

import java.io._

class ContractDataStoreHandler extends ContractDataStore.Handler[Stack] with LogSupport {
  private val contractDataFileName                                  = "contractdata"
  private val contractDataTrie: SafeVar[Trie[Char, String]]         = SafeVar.empty
  private val contractDataStore: Once[LevelDBStore[String, String]] = Once.empty
  private val contractDataTrieJsonFile: Once[ScalaFile]             = Once.empty

  override def initializeContractDataStore(dataDir: File): Stack[Unit] = Stack { setting =>
    val path = new File(dataDir, contractDataFileName)
    path.mkdirs()
    contractDataTrieJsonFile := new File(path, "contractdata.trie.json").toScala
    contractDataTrieJsonFile.foreach { f =>
      if (f.exists && f.isRegularFile) {
        //reload
        val reloadResult = for {
          json <- parse(f.contentAsString(Charset.forName("utf-8")))
          trie <- json.as[Trie[Char, String]]
        } yield trie

        reloadResult match {
          case Left(t) =>
            log.error("reload contract data trie faield", t)
            throw t
          case Right(trie) =>
            contractDataTrie := trie
            log.info(s"reloaded contract data trie, root hash = ${trie.rootHexHash}")
        }
      } else if (f.notExists) {
        //init
        val trie = Trie.empty[Char, String]
        f.overwrite(trie.asJson.spaces2)
        contractDataTrie := trie
        log.info("init contract data trie.")
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

  /** commit staged tokens
    */
  override def commitStagedContractData(height: BigInt): Stack[Unit] = Stack { setting =>
    //todo: staged data now is not write to a temp store. SHOULD be fixed

    }

  /** rollback staged tokens
    */
  override def rollbackStagedContractData(height: BigInt): Stack[Unit] = Stack { setting =>
    }

  /** prepare a sql store for running a specified contract
    */
  override def prepareSqlStoreFor(height: BigInt,
                                  contract: Contract.UserContract): Stack[SqlStore] = Stack {
    setting =>
      setting match {
        case x: Setting.P2PNodeSetting =>
          val contractWorkingDir =
            new File(new File(x.workingDir, contractDataFileName), contract.name.value)
          val dbPath = new File(contractWorkingDir, "sqlstore")
          dbPath.mkdirs()
          val dbUrl = s"jdbc:h2:${dbPath.getAbsolutePath}/db"
          new H2SqlStore(dbUrl)

        case _ => throw new RuntimeException("should working in CoreNode or EdgeNode")
      }
  }

  /** close a sql store
    */
  override def closeSqlStore(sqlStore: SqlStore): Stack[Unit] = Stack { setting =>
    sqlStore match {
      case x: H2SqlStore => x.close()
      case _             => // nothing to do.
    }
  }

  /** prepare a key value store for running a specified contract
    */
  override def prepareKeyValueStoreFor(height: BigInt,
                                       contract: Contract.UserContract): Stack[KVStore] = Stack {
    setting =>
      setting match {
        case x: Setting.P2PNodeSetting =>
          val contractWorkingDir =
            new File(new File(x.workingDir, contractDataFileName), contract.name.value)
          val kvPath = new File(contractWorkingDir, "kvstore")
          kvPath.mkdirs()
          new LevelDBKVStore(kvPath)

        case _ => throw new RuntimeException("should working in CoreNode or EdgeNode")
      }
  }

  /** close a kv store
    */
  override def closeKeyValueStore(kvStore: KVStore): Stack[Unit] = Stack { setting =>
    kvStore match {
      case x: LevelDBKVStore => x.close()
      case _                 => // nothing to do.
    }
  }
}

object ContractDataStoreHandler {
  private val instance = new ContractDataStoreHandler()

  trait Implicits {
    implicit val contractDataStoreHandlerInstance: ContractDataStoreHandler = instance
  }
}
