package fssi
package interpreter

import types._, implicits._
import utils._, trie._
import ast._

import java.io._

class ContractDataStoreHandler extends ContractDataStore.Handler[Stack] {
  private val contractDataFileName         = "contractdata"
  private val contractDataTrie: Once[Trie] = Once.empty

  override def initializeContractDataStore(dataDir: File): Stack[Unit] = Stack {
    val path = new File(dataDir, contractDataFileName)
    path.mkdirs()
    contractDataTrie := Trie.empty(levelDBStore(path))
  }

  /** self test for a contract data store
    * @return if the store is sane return true, or false
    */
  override def testContractDataStore(): Stack[Boolean] = Stack { setting =>
    //todo: should check the store
    true
  }

  /** get current contract data store state
    * this state should identify current state of contract data store
    */
  override def getContractDataStoreState(): Stack[String] = Stack { setting =>
    //todo: use the root hash of the trie
    ""
  }

  /** verify current state of contract data store
    */
  override def verifyContractDataStoreState(state: String): Stack[Boolean] = Stack { setting =>
    //todo: verify the trie
    true
  }
}

object ContractDataStoreHandler {
  private val instance = new ContractDataStoreHandler()

  trait Implicits {
    implicit val contractDataStoreHandlerInstance: ContractDataStoreHandler = instance
  }
}
