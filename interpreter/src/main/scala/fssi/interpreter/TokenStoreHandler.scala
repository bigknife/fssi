package fssi
package interpreter

import types._, implicits._
import utils._, trie._
import ast._

import java.io._

class TokenStoreHandler extends TokenStore.Handler[Stack] with LogSupport {
  private val tokenFileDirName      = "token"
  private val tokenTrie: Once[Trie] = Once.empty

  override def initializeTokenStore(dataDir: File): Stack[Unit] = Stack { setting =>
    val path = new File(dataDir, tokenFileDirName)
    path.mkdirs()
    tokenTrie := Trie.empty(levelDBStore(path))
  }

  /** self test for a token store
    * @param block token store should be tested on block
    * @return if the store is sane return true, or false
    */
  override def testTokenStore(block: Block): Stack[Boolean] = Stack { setting =>
    tokenTrie
      .map { trie =>
        val allEmpty = (block.previousTokenState.isEmpty) && trie.hash.isEmpty
        val sameHash = trie.hash.isDefined && (block.previousTokenState.bytes sameElements trie.hash.get)
        allEmpty || sameHash
      }
      .toOption
      .getOrElse(false)
  }

  /** get current token store state
    * this state should identify current state of token store
    */
  override def getTokenStoreState(): Stack[HexString] = Stack { setting =>
    // should use the root hash of the trie
    tokenTrie.map { _.hash }.toOption.flatten.map(HexString(_)).getOrElse(HexString.empty)
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
