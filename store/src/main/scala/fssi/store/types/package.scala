package fssi.store

import scala.collection.immutable._

package object types {
  type StoreKeySet = TreeSet[StoreKey]
  object StoreKeySet {
    def empty: StoreKeySet                        = TreeSet.empty[StoreKey]
    def apply(keys: Array[StoreKey]): StoreKeySet = TreeSet(keys.toSeq: _*)

    def same(x1: StoreKeySet, x2: StoreKeySet): Boolean = !x1.exists { a =>
      x2.exists(b => !(a === b))
    }

    def toBytes(set: StoreKeySet): Array[Byte] = {
      set.map(_.stringValue).mkString("\n").getBytes("utf-8")
    }

    def fromBytes(bytes: Array[Byte]): StoreKeySet = {
      apply(new String(bytes, "utf-8").split("\n").map(StoreKey.parse))
    }
  }
}
