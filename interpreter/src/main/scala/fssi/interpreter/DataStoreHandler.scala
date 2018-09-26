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

class DataStoreHandler extends DataStore.Handler[Stack] with LogSupport {
  private val dataTrie: SafeVar[Trie[Char, String]] = SafeVar.empty
  private val dataStore: Once[LevelDBStore[String, String]] = Once.empty
  private val dataTrieJsonFile: Once[ScalaFile] = Once.empty

  /** init data store
    */
  override def initializeDataStore(dataStoreRoot: File): Stack[Unit] = Stack {
    dataTrieJsonFile := new File(dataStoreRoot, "data.trie.json").toScala
    dataTrieJsonFile.foreach {f =>
      if(f.exists && f.isRegularFile) {
        //reload
        val reloadResult = for {
          json <- parse(f.contentAsString(Charset.forName("utf-8")))
          trie <- json.as[Trie[Char, String]]
        } yield trie

        reloadResult match {
          case Left(t) =>
            log.error("reload data trie failed", t)
            throw t
          case Right(trie) =>
            if (trie.isEmpty)
              throw new RuntimeException("data trie is empty, check your working directory.")
            else {
              dataTrie := trie
              log.info(s"reloaed data trie, root hash = ${trie.rootHexHash}")
            }
        }
      }
      else if (f.notExists) {
        //init
        val trie = Trie.empty[Char, String]
        f.overwrite(trie.asJson.spaces2)
        dataTrie := trie
        log.info("init data trie.")
      } else throw new RuntimeException(s"$f should be empty to init or a regular file to load.")
    }

    val dbFile = new File(dataStoreRoot, "db")
    dbFile.mkdirs()
    dataStore := levelDBStore(dbFile)
    log.info(s"init leveldb at $dbFile")
  }
}

object DataStoreHandler {
   val instance = new DataStoreHandler

  trait Implicits {
    implicit val dataStoreHandlerInstance: DataStoreHandler = instance
  }
}
