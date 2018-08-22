package fssi
package interpreter

import types._, implicits._
import utils._, trie._
import ast._

import java.io._

class ContractStoreHandler extends ContractStore.Handler[Stack] {
  private val contractFileDirName      = "contract"
  private val contractTrie: Once[Trie] = Once.empty

  /** initialize a data directory to be a contract store
    * @param dataDir directory to save contract.
    */
  override def initializeContractStore(dataDir: File): Stack[Unit] = Stack {
    val path = new File(dataDir, contractFileDirName)
    path.mkdirs()
    contractTrie := Trie.empty(levelDBStore(path))
  }

  /** self test for a contract store
    * @return if the store is sane return true, or false
    */
  override def testContractStore(): Stack[Boolean] = Stack { setting =>
    //todo should check the trie
    true
  }

  /** get current token store state
    * this state should identify current state of token store
    */
  override def getTokenStoreState(): Stack[String] = Stack { setting =>
    //todo use the root the trie
    ""
  }

  /** verify current state of contract store
    */
  override def verifyContractStoreState(state: String): Stack[Boolean] = Stack { setting =>
    //todo: verify the trie
    true
  }

}

object ContractStoreHandler {
  private val instance = new ContractStoreHandler()

  trait Implicits {
    implicit val contractStoreHandlerInstance: ContractStoreHandler = instance
  }
}
