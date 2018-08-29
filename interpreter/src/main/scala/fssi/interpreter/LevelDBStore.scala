package fssi
package interpreter

import java.io._
import trie._

/** store based on leveldb
  *
  */
trait LevelDBStore[K, V] {
  implicit val BK: Bytes[K]
  implicit val BV: Bytes[V]

  import org.iq80.leveldb._
  val dbFile: File

  private lazy val db: DB = {
    require(dbFile.exists() && dbFile.isDirectory, "dbFile should be a directory")

    val options: Options = new Options()
    options.createIfMissing(true)
    org.fusesource.leveldbjni.JniDBFactory.factory.open(dbFile, options)
  }

  def save(key: K, value: V): Unit = db.put(BK.determinedBytes(key), BV.determinedBytes(value))

  def load(key: K): Option[V] = {
    Option(db.get(BK.determinedBytes(key))).map(BV.to)
  }

  def delete(key: K): Unit = db.delete(BK.determinedBytes(key))

  def shutdown(): Unit = db.close()
}
