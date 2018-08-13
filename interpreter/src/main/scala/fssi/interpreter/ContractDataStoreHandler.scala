package fssi
package interpreter

import types._, implicits._
import utils._, trie._
import ast._

import java.io._

class ContractDataStoreHandler extends ContractDataStore.Handler[Stack] {
  private val contractDataFileName         = "contractdata"
  private val contractDataTrie: Once[Trie] = Once.empty

  override def initialize(dataDir: File): Stack[Unit] = Stack {
    val path = new File(dataDir, contractDataFileName)
    path.mkdirs()
    contractDataTrie := Trie.empty(levelDBStore(path))
  }
}

object ContractDataStoreHandler {
  private val instance = new ContractDataStoreHandler()

  trait Implicits {
    implicit val contractDataStoreHandlerInstance: ContractDataStoreHandler = instance
  }
}
