package fssi
package interpreter

import contract.lib._
import java.io._
import trie.Bytes.implicits._

class LevelDBKVStore(rootPath: File) extends KVStore {
  lazy val ldb = {
    val dbFile = new File(rootPath, "db")
    dbFile.mkdirs()
    levelDBStore[Array[Byte], Array[Byte]](dbFile)
  }

  def put(key: Array[Byte], value: Array[Byte]): Unit = ldb.save(key, value)
  def get(key: Array[Byte]): Array[Byte] = ldb.load(key).getOrElse(null)

  def close(): Unit = ldb.shutdown()
}
