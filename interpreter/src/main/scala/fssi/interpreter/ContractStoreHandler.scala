package fssi
package interpreter

import jsonCodecs._
import utils._
import trie._
import types.biz._
import types.base._
import types.implicits._
import types.exception._

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
  private val contractStage: SafeVar[Map[BigInt, Map[String, Contract.UserContract]]] =
    SafeVar(Map.empty)

  /** initialize a data directory to be a contract store
    * @param dataDir directory to save contract.
    */
  /*
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
   */

  /** self test for a contract store
    * @param block contract store should be tested on block
    * @return if the store is sane return true, or false
    */
  /*
  override def testContractStore(block: Block): Stack[Boolean] = Stack { setting =>
    true
  }
   */

  /** verify current state of contract store
    */
  /*
  override def verifyContractStoreState(state: String): Stack[Boolean] = Stack { setting =>
    true
  }
   */

  /** commit staged tokens
    */
  /*
  override def commitStagedContract(height: BigInt): Stack[Unit] = Stack { setting =>
    // gid -> leveldbkey -> leveldb value
    contractTrie.updated { trie =>
      val contractMap: Map[String, Contract.UserContract] =
        contractStage.map(_.get(height)).value.getOrElse(Map.empty)
      contractMap
        .foldLeft(trie) {
          case (acc, (gid, contract)) =>
            val contractKey   = contract.signature.value.toString
            val contractValue = contract.asJson.noSpaces
            val gidKey        = HexString(gid.getBytes("utf-8")).noPrefix
            val nextTrie      = acc.put(gidKey.toCharArray, contractKey)
            contractStore.foreach { store =>
              store.save(contractKey, contractValue)
            }
            nextTrie
        }
        .unsafe { trieFinal =>
          contractTrieJsonFile.foreach{
            f => f.overwrite(trieFinal.asJson.spaces2)
            log.info(s"saved contract json file to $f")
          }
        }
    }
  }
   */

  /** rollback staged tokens
    */
  /*
  override def rollbackStagedContract(height: BigInt): Stack[Unit] = Stack { setting =>
    contractStage.updated { map =>
      map - height
    }
  }
   */

  /** temp save user's contract
    */
  /*
  override def stageContract(height: BigInt,
                             gid: String,
                             contract: Contract.UserContract): Stack[Unit] = Stack { setting =>
    contractStage.updated { map =>
      val m = map.get(height).getOrElse(Map.empty) + (gid -> contract)
      map + (height -> m)
    }
  }
   */

  /** find user contract with gid
    */
  /*
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
   */

    /** load a uer contract from a proper contract file
    */
  override  def loadUserContract(contractFile: File): Stack[Either[FSSIException, Contract.UserContract]] = Stack {setting =>
    //todo: mock
    Right(Contract.UserContract(
      owner = Account.emptyId,
      name = UniqueName.empty,
      version = Contract.Version.empty,
      code = Contract.UserContract.Code(Array.emptyByteArray),
      methods = immutable.TreeSet.empty[Contract.UserContract.Method],
      signature = Signature.empty
    ))
  }

}

object ContractStoreHandler {
  private val instance = new ContractStoreHandler()

  trait Implicits {
    implicit val contractStoreHandlerInstance: ContractStoreHandler = instance
  }
}
