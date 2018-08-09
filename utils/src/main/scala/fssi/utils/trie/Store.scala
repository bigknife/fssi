package fssi.utils
package trie

import java.io.File

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
    */
  def shutdown(): Unit = ()
}

object Store {
  trait MemStore extends Store {
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

  /** create a memory based store
    *
    * @return store
    */
  def memory(): Store = new MemStore {}

}
