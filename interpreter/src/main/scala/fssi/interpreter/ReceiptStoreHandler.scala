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

class ReceiptStoreHandler extends ReceiptStore.Handler[Stack] with LogSupport {
  private val receiptTrie: SafeVar[Trie[Char, String]] = SafeVar.empty
  private val receiptStore: Once[LevelDBStore[String, String]] = Once.empty
  private val receiptTrieJsonFile: Once[ScalaFile] = Once.empty

  /** init receipt store
    */
  override def initializeReceiptStore(receiptStoreRoot: File): Stack[Unit] = Stack {
    receiptTrieJsonFile := new File(receiptStoreRoot, "receipt.trie.json").toScala
    receiptTrieJsonFile.foreach {f =>
      if(f.exists && f.isRegularFile) {
        //reload
        val reloadResult = for {
          json <- parse(f.contentAsString(Charset.forName("utf-8")))
          trie <- json.as[Trie[Char, String]]
        } yield trie

        reloadResult match {
          case Left(t) =>
            log.error("reload receipt trie failed", t)
            throw t
          case Right(trie) =>
            if (trie.isEmpty)
              throw new RuntimeException("receipt trie is empty, check your working directory.")
            else {
              receiptTrie := trie
              log.info(s"reloaed receipt trie, root hash = ${trie.rootHexHash}")
            }
        }
      }
      else if (f.notExists) {
        //init
        val trie = Trie.empty[Char, String]
        f.overwrite(trie.asJson.spaces2)
        receiptTrie := trie
        log.info("init receipt trie.")
      } else throw new RuntimeException(s"$f should be empty to init or a regular file to load.")
    }

    val dbFile = new File(receiptStoreRoot, "db")
    dbFile.mkdirs()
    receiptStore := levelDBStore(dbFile)
    log.info(s"init leveldb at $dbFile")
  }
}

object ReceiptStoreHandler {
  val instance = new ReceiptStoreHandler

  trait Implicits {
    implicit val receiptStoreHandlerInstance: ReceiptStoreHandler = instance
  }
}
