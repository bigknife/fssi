package fssi
package interpreter

import types._, implicits._
import utils._, trie._
import ast._

import java.io._

class ContractStoreHandler extends ContractStore.Handler[Stack] {
  private val contractFileDirName = "contract"
  private val contractTrie: Once[Trie] = Once.empty

  override def initialize(dataDir: File): Stack[Unit] = Stack {
    val path = new File(dataDir, contractFileDirName)
    path.mkdirs()
    contractTrie := Trie.empty(levelDBStore(path))
  }
}

object ContractStoreHandler {
  private val instance = new ContractStoreHandler()

  trait Implicits {
    implicit val contractStoreHandlerInstance: ContractStoreHandler = instance
  }
}
