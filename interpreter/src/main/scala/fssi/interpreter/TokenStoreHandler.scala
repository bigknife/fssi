package fssi
package interpreter

import contract.lib._
import types._
import implicits._
import utils._
import trie._
import Bytes.implicits._
import ast._
import java.io._
import java.nio.charset.Charset

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
import Bytes.implicits._
import better.files.{File => ScalaFile, _}
import java.io._
import java.lang

class TokenStoreHandler extends TokenStore.Handler[Stack] with LogSupport {
  private val tokenFileDirName                                         = "token"
  private val tokenTrie: SafeVar[Trie[Char, String]]                   = SafeVar.empty
  private val tokenStore: Once[LevelDBStore[String, String]]           = Once.empty
  private val tokenTrieJsonFile: Once[ScalaFile]                       = Once.empty
  private val tokenStage: SafeVar[Map[BigInt, Map[Account.ID, Token]]] = SafeVar(Map.empty)

  override def initializeTokenStore(tokenStoreRoot: File): Stack[Unit] = Stack { setting =>
    // find token.trie(json) to initialize a trie
    // if not found, create one and persist it.
    tokenTrieJsonFile := new File(tokenStoreRoot, "token.trie.json").toScala
    tokenTrieJsonFile.foreach { f =>
      if (f.exists && f.isRegularFile) {
        //reload
        val reloadResult = for {
          json <- parse(f.contentAsString(Charset.forName("utf-8")))
          trie <- json.as[Trie[Char, String]]
        } yield trie

        reloadResult match {
          case Left(t) =>
            log.error("reload token trie faield", t)
            throw t
          case Right(trie) =>
            tokenTrie := trie
            log.info(s"reloaded token trie, root hash = ${trie.rootHexHash}")
        }
      } else if (f.notExists) {
        //init
        val trie = Trie.empty[Char, String]
        f.overwrite(trie.asJson.spaces2)
        tokenTrie := trie
        log.info("init token trie.")
      } else
        throw new RuntimeException(s"$f should be empty to init or a regular file to load.")

      // init or load level db store
      val dbFile = new File(tokenStoreRoot, "db")
      dbFile.mkdirs()
      tokenStore := levelDBStore(dbFile)
      log.info(s"init leveldb at $dbFile")
    }

  }

  /** self test for a token store
    * @param block token store should be tested on block
    * @return if the store is sane return true, or false
    */
  override def testTokenStore(block: Block): Stack[Boolean] = Stack { setting =>
    // if the trie was reloaded, it should be sane.
    true
  }

  /** get current token store state
    * this state should identify current state of token store
    */
  override def getTokenStoreState(): Stack[HexString] = Stack { setting =>
    // should use the root hash of the trie
    tokenTrie.map { _.rootHexHash }.map(HexString.decode).unsafe()
  }

  /** verify current state of token store
    */
  override def verifyTokenStoreState(state: String): Stack[Boolean] = Stack { setting =>
    tokenTrie
      .map(_.rootHash)
      .map(HexString(_))
      .map(_ == HexString.decode(state))
      .getOrElse(default = false)
  }

  override def getCurrentToken(account: Account.ID): Stack[Token] = Stack { setting =>
    // accountId -> tokenHash -> token
    val accountIdKey = account.value.noPrefix.toCharArray
    tokenTrie
      .map { trie =>
        trie
          .get(accountIdKey)
          .map(tokenHash =>
            tokenStore.map(_.load(tokenHash).getOrElse(Token.Zero.toString)).unsafe())
          .map(x => Token.parse(x))
          .getOrElse(Token.Zero)
      }
      .getOrElse(Token.Zero)
  }

  /** stage the account's token
    */
  override def stageToken(height: BigInt, account: Account.ID, token: Token): Stack[Unit] = Stack {
    setting =>
      tokenStage.updated { x =>
        val m: Map[Account.ID, Token] = x.getOrElse(height, Map.empty)
        x + (height -> (m + (account -> token)))
      }
  }

  /** commit staged tokens
    */
  override def commitStagedToken(height: BigInt): Stack[Unit] = Stack { setting =>
    val tokenMap: Map[Account.ID, Token] =
      tokenStage.map(_.getOrElse(height, Map.empty[Account.ID, Token])).value
    // write to the trie and store
    tokenTrie.updated { trie =>
      tokenMap.foldLeft(trie) { (acc, n) =>
        val k     = n._1.value.noPrefix.toCharArray
        val v     = n._2.toString
        val vHash = BytesUtil.toHex(crypto.hash(v.getBytes("utf-8")))

        // vHash -> v saved to store
        tokenStore.foreach { store =>
          store.save(vHash, v)
        }
        acc.put(k, vHash)
      }.unsafe {trie =>
        tokenTrieJsonFile.foreach{ f =>
          f.overwrite(trie.asJson.spaces2)
          log.info(s"saved token json file to $f")
        }
      }
    }
  }

  /** rollback staged tokens
    */
  override def rollbackStagedToken(height: BigInt): Stack[Unit] = Stack { setting =>
    tokenStage.updated { stage =>
      stage.filterKeys(_ != height)
    }
  }

  /** prepare a token query for running a specified contract
    */
  override def prepareTokenQueryFor(height: BigInt,
                                    contract: Contract.UserContract): Stack[TokenQuery] = Stack {
    setting =>
      new TokenQuery {

        /**
          * get amount of an account
          *
          * @return token amount, with the base unit.
          */
        override def getAmount(accountId: String): lang.Long = {
          // first check stage, if not found ,check store
          val aid = Account.ID(HexString.decode(accountId))
          tokenStage
            .map { m =>
              (for {
                mm <- m.get(height)
                t  <- mm.get(aid)
              } yield t).map(_.amount.toLong: java.lang.Long)
            }
            .value
            .orElse(tokenTrie.map(_.get(aid.value.noPrefix.toCharArray)).value.flatMap { vHash =>
              tokenStore
                .map(_.load(vHash))
                .value
                .map(Token.parse)
                .map(_.amount.toLong: java.lang.Long)
            })
            .getOrElse(0L)
        }
      }
  }
}

object TokenStoreHandler {
  private val instance = new TokenStoreHandler()

  trait Implicits {
    implicit val tokenStoreHandlerInstance: TokenStoreHandler = instance
  }
}
