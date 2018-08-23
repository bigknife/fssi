package fssi
package interpreter

import utils.trie._
import java.io._

/** store based on leveldb
  *
  */
trait LevelDBStore extends Store {
  import org.iq80.leveldb._
  val dbFile: File

  private lazy val db: DB = {
    require(dbFile.exists() && dbFile.isDirectory, "dbFile should be a directory")

    val options: Options = new Options()
    options.createIfMissing(true)
    org.fusesource.leveldbjni.JniDBFactory.factory.open(dbFile, options)
  }

  override def save(key: StoreKey, value: StoreValue): Unit = db.put(key, value)

  override def load(key: StoreKey): Option[StoreValue] = {
    Option(db.get(key))
  }

  override def delete(key: StoreKey): Unit = db.delete(key)

  override def shutdown(): Unit = db.close()
}
