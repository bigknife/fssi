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

  /** find user contract with gid
    */
  override def findUserContract(name: UniqueName,
                                version: Version): Stack[Option[Contract.UserContract]] = Stack {
    setting =>
      // in trie, hex(name + version) -> contract-hash -> leveldbstore
      val gid    = s"${name.value}#${version.value}"
      val gidKey = HexString(gid.getBytes("utf-8")).noPrefix
      contractTrie
        .map { trie =>
          trie.get(gidKey.toCharArray).map { storeKey =>
            contractStore.map { store =>
              store.load(storeKey)
            }.value
          }
        }
        .value
        .flatten
        .flatMap { str =>
          (for {
            json     <- parse(str)
            contract <- json.as[Contract.UserContract]
          } yield contract).toOption
        }
  }

  /** prepare a sql store for running a specified contract
    */
  override def prepareSqlStoreFor(height: BigInt,
                                  contract: Contract.UserContract): Stack[SqlStore] = Stack {
    setting =>
      setting match {
        case x: Setting.P2PNodeSetting =>
          val contractWorkingDir =
            new File(new File(x.workingDir, contractFileDirName), contract.name.value)
          val dbPath = new File(contractWorkingDir, "sqlstore")
          dbPath.mkdirs()
          val dbUrl = s"jdbc:h2:${dbPath.getAbsolutePath}"
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
            new File(new File(x.workingDir, contractFileDirName), contract.name.value)
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
      case _ => // nothing to do.
    }
  }
}

object ContractStoreHandler {
  private val instance = new ContractStoreHandler()

  trait Implicits {
    implicit val contractStoreHandlerInstance: ContractStoreHandler = instance
  }
}
