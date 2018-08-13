package fssi
package interpreter

import types._, implicits._
import utils._, trie._
import ast._

import java.io._

class TokenStoreHandler extends TokenStore.Handler[Stack] {
  private val tokenFileDirName = "token"
  private val tokenTrie: Once[Trie] = Once.empty

  override def initialize(dataDir: File): Stack[Unit] = Stack { setting =>
    val path = new File(dataDir, tokenFileDirName)
    path.mkdirs()
    tokenTrie := Trie.empty(levelDBStore(path))
  }
}

object TokenStoreHandler {
  private val instance = new TokenStoreHandler()

  trait Implicits {
    implicit val tokenStoreHandlerInstance: TokenStoreHandler = instance
  }
}
