package fssi.interpreter.util.trie

import java.io.File

import fssi.ast.domain.types.BytesValue
import scala.collection._

/** store, to save key - value
  * and, the key and value is encoded to Array[Byte]
  *
  */
trait Store {
  def save(key: StoreKey, value: StoreValue): Unit
  def load(key: StoreKey): Option[StoreValue]
  def delete(key: StoreKey): Unit

  /** shut down hook
    *
    */
  def shutdown(): Unit = ()
}

object Store {
  trait MemStore extends Store{
    private val map = mutable.Map.empty[String, StoreValue]

    override def save(key: StoreKey, value: StoreValue): Unit = {
      val k = BytesValue(key).hex
      map.put(k, value)
      //println(s"saved: $k")
      ()
    }

    override def load(key: StoreKey): Option[StoreValue] = {
      val k = BytesValue(key).hex
      val s = map.get(k)
      //println(s"load: $k -> $s")
      s
    }

    override def delete(key: StoreKey): Unit = {
      val k = BytesValue(key).hex
      map.remove(k)
      ()
    }

    override def toString: String = "MemoryStore"
  }

  /** store based on leveldb
    *
    */
  trait LevelDBStore extends Store {
    import org.iq80.leveldb._
    val dbFile: java.io.File

    private lazy val db: DB = {
      require(dbFile.exists() && dbFile.isDirectory, "dbFile should be a directory")

      val options: Options = new Options()
      options.createIfMissing(true)
      org.fusesource.leveldbjni.JniDBFactory.factory.open(dbFile, options)
    }

    override def save(key: StoreKey, value: StoreValue): Unit = db.put(key, value)


    override def load(key: StoreKey): Option[StoreValue] = Option(db.get(key))

    override def delete(key: StoreKey): Unit = db.delete(key)

    override def shutdown(): Unit = db.close()
  }

  /** create a memory based store
    *
    * @return store
    */
  def memory(): Store = new MemStore {}

  def levelDB(path: String): Store = new LevelDBStore {
    override val dbFile: File = {
      val f = new java.io.File(path)
      if (!f.exists()) {
        f.mkdirs()
      }
      else require(f.isDirectory)

      f
    }
  }

}
