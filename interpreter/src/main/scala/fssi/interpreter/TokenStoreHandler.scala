package fssi
package interpreter

import types._, implicits._
import utils._, trie._
import ast._

import java.io._

class TokenStoreHandler extends TokenStore.Handler[Stack] {
  private val tokenFileDirName      = "token"
  private val tokenTrie: Once[Trie] = Once.empty

  override def initializeTokenStore(dataDir: File): Stack[Unit] = Stack { setting =>
    val path = new File(dataDir, tokenFileDirName)
    path.mkdirs()
    tokenTrie := Trie.empty(levelDBStore(path))
  }

  /** self test for a token store
    * @return if the store is sane return true, or false
    */
  override def testTokenStore(): Stack[Boolean] = Stack { setting =>
    //todo: should check the trie
    true
  }

  /** get current token store state
    * this state should identify current state of token store
    */
  override def getTokenStoreState(): Stack[String] = Stack { setting =>
    // should use the root hash of the trie
    ""
  }

  /** verify current state of token store
    */
  override def verifyTokenStoreState(state: String): Stack[Boolean] = Stack { setting =>
    //todo: verify the trie
    true
  }
}

object TokenStoreHandler {
  private val instance = new TokenStoreHandler()

  trait Implicits {
    implicit val tokenStoreHandlerInstance: TokenStoreHandler = instance
  }
}
