package fssi
package trie

import shapeless.Data
import utils._

import scala.reflect.ClassTag

sealed trait Node[K, V] {

  /** put a k-v to the node
    * @return new node contains k-v data and old node
    */
  def put(key: Array[K], data: V): Node[K, V]

  /** get a value with key from node
    * @return optional data
    */
  def get(key: Array[K]): Option[V]

  /* this node's hash (this node is a node of hash tree)
   */
  def hash(implicit BK: Bytes[K], BV: Bytes[V]): Array[Byte]

  /** encode hash into Hex string
    */
  def hexHash(implicit BK: Bytes[K], BV: Bytes[V]): String =
    s"0x${hash.map("%02x" format _).mkString("")}"
}

object Node {

  def apply[K: ClassTag, V](keys: Array[K], value: V): Node[K, V] = {
    require(keys.length > 0)
    if (keys.length == 1) {
      Slot(keys(0), Some(value), None)
    } else {
      Compact(keys, Some(value), None)
    }

  }

  def branch[K, V](slots: Slot[K, V]*): Branch[K, V] = Branch(slots.map(x => x.idx -> x).toMap)

  case class Slot[K: ClassTag, V](idx: K, data: Option[V], node: Option[Node[K, V]])
      extends Node[K, V] {

    /** put a k-v to the node
      *
      * @return new node contains k-v data and old node
      */
    override def put(key: Array[K], _data: V): Node[K, V] = {
      require(key.length > 0)
      if (key.length == 1 && key(0) == idx) copy(data = Some(_data))
      else if (key.head == idx) {
        copy(node =
          node.map(_.put(key.drop(1), _data)).orElse(Some(Compact(key.drop(1), Some(_data), None))))
      } else {
        val slot =
          if (key.length == 0) Slot(key(0), Some(_data), None)
          else
            Slot(key(0), None, Some(Compact(key.drop(1), Some(_data), None)))
        branch(this, slot)
      }
    }

    /** get a value with key from node
      *
      * @return optional data
      */
    override def get(key: Array[K]): Option[V] =
      if (key.length == 1 && key(0) == idx) data
      else if (key.length > 1) {
        node.flatMap(_.get(key.drop(1)))
      } else None

    override def hash(implicit BK: Bytes[K], BV: Bytes[V]): Array[Byte] = {
      import Bytes.implicits._
      (idx.hash ++ data.map(_.hash).getOrElse(Array.emptyByteArray) ++ node
        .map(_.hash)
        .getOrElse(Array.emptyByteArray)).hash
    }

    override def toString: String =
      if (node.isDefined) s"Slot($idx -> $data, $node)" else s"Slot($idx -> $data)"
  }

  case class Compact[K: ClassTag, V](indexes: Array[K], data: Option[V], node: Option[Node[K, V]])
      extends Node[K, V] {

    /** put a k-v to the node
      *
      * @return new node contains k-v data and old node
      */
    override def put(key: Array[K], _data: V): Node[K, V] = {
      require(key.length > 0)
      // totally eq
      if (key sameElements indexes) copy(data = Some(_data))
      else {
        val prefix = maxPrefix(key, indexes)
        if (prefix.length > 0) {
          if (prefix sameElements key) {
            // key is prefix of indexes
            Compact(prefix, Some(_data), Some(copy(indexes = indexes.drop(prefix.length))))
          } else if (prefix sameElements indexes) {
            val n = Compact(key.drop(prefix.length), Some(_data), None)
            // add n to this node
            copy(node = node.map(_.put(key.drop(prefix.length), _data)).orElse(Some(n)))
          } else {
            val n: Node[K, V] = {
              val k1 = indexes.drop(prefix.length)
              val k2 = key.drop(prefix.length)
              val slot1 =
                if (k1.length == 1) Slot(k1(0), data, node)
                else
                  Slot(k1(0), None, Some(Compact(k1.drop(1), data, node)))
              val slot2 =
                if (k2.length == 1) Slot(k2(0), Some(_data), None)
                else
                  Slot(k2(0), None, Some(Compact(k2.drop(1), Some(_data), None)))
              Branch(Map(slot1.idx -> slot1, slot2.idx -> slot2))
            }
            Compact(prefix, None, Some(n))
          }
        } else {
          val slot1 = Slot(indexes.head, None, Some(copy(indexes = indexes.drop(1))))
          val slot2 = Slot(key.head, None, Some(Compact(key.drop(1), Some(_data), None)))
          Branch(Map(slot1.idx -> slot1, slot2.idx -> slot2))
        }
      }
    }

    /** get a value with key from node
      *
      * @return optional data
      */
    override def get(key: Array[K]): Option[V] = {
      if (key sameElements indexes) data
      else {
        node.flatMap(_.get(key.drop(key.length)))
      }
    }
    override def hash(implicit BK: Bytes[K], BV: Bytes[V]): Array[Byte] = {
      import Bytes.implicits._
      (indexes.map(_.determinedBytes).fold(Array.emptyByteArray)(_ ++ _).hash ++
        data.map(_.hash).getOrElse(Array.emptyByteArray) ++ node
        .map(_.hash)
        .getOrElse(Array.emptyByteArray)).hash
    }

    override def toString: String =
      if (node.isDefined) s"Compact(${indexes.map(_.toString).mkString(",")} -> $data, $node)"
      else s"Compact(${indexes.map(_.toString).mkString(",")} -> $data))"
  }

  case class Branch[K, V](slots: Map[K, Slot[K, V]]) extends Node[K, V] {

    /** put a k-v to the node
      *
      * @return new node contains k-v data and old node
      */
    override def put(key: Array[K], data: V): Node[K, V] = ???

    /** get a value with key from node
      *
      * @return optional data
      */
    override def get(key: Array[K]): Option[V] = {
      if (key.length == 0) None
      else {
        slots.get(key.head).flatMap(_.get(key))
      }
    }
    override def hash(implicit BK: Bytes[K], BV: Bytes[V]): Array[Byte] = {
      import Bytes.implicits._
      val sortedSlots = slots.toVector
        .sortBy {
          case (k, _) => BK.hash(k).map("%02x" format _).mkString("")
        }
        .map(_._2)

      sortedSlots.map(_.hash).fold(Array.emptyByteArray)(_ ++ _).hash
    }
  }

  def maxPrefix[K: ClassTag](k1: Array[K], k2: Array[K]): Array[K] = {
    def loop(k1: Array[K], k2: Array[K], acc: Array[K]): Array[K] =
      if (k1.length > 0 && k2.length > 0) {
        val h1 = k1.head
        val h2 = k2.head
        if (h1 == h2) loop(k1.drop(1), k2.drop(1), acc :+ h1)
        else acc
      } else acc
    loop(k1, k2, Array.empty[K])
  }

}
