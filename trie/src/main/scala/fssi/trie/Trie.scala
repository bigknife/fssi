package fssi
package trie

import scala.reflect.ClassTag

sealed trait Trie[K, V] {
  def put(key: Array[K], data: V): Trie[K, V]
  def get(key: Array[K]): Option[V]
  def rootHash(implicit BK: Bytes[K], BV: Bytes[V]): Array[Byte]
  def rootHexHash(implicit BK: Bytes[K], BV: Bytes[V]): String = s"0x${rootHash.map("%02x" format _).mkString("")}"
}

object Trie {

  case class SimpleTrie[K: ClassTag, V](root: Option[Node[K, V]]) extends Trie[K, V] {
    override def put(key: Array[K], data: V): Trie[K, V] = {
      copy(root = root.map(_.put(key, data)).orElse(Some(Node(key, data))))
    }

    override def get(key: Array[K]): Option[V] = {
      root.flatMap(_.get(key))
    }

    def rootHash(implicit BK: Bytes[K], BV: Bytes[V]): Array[Byte] = root.map(_.hash).getOrElse(Array.emptyByteArray)
  }

  def apply[K: ClassTag, V](implicit EK: Bytes[K], EV: Bytes[V]): Trie[K, V] = SimpleTrie[K, V](None)
  def empty[K: ClassTag, V](implicit EK: Bytes[K], EV: Bytes[V]): Trie[K,V]   = apply[K, V]
  def apply[K: ClassTag, V](key: Array[K], data: V)(implicit EK: Bytes[K], EV: Bytes[V]): Trie[K, V] =
    SimpleTrie(Some(Node(key, data)))
  def apply[K: ClassTag,V](node: Node[K, V])(implicit EK: Bytes[K], EV:Bytes[V]): Trie[K, V] = SimpleTrie(Some(node))

}
