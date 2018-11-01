package fssi.store
package types

import java.nio.ByteBuffer

case class StoreValue(bytes: Array[Byte], childrenKeys: StoreKeySet, hash: Array[Byte]) {
  /** serialize all data to an array
    * add 3 * 4 = 12 bytes, or, 3 Int, to express the length of three fields
    */
  def serialized: Array[Byte] = {
    val childrenKeysBytes = childrenKeys.mkString("\n").getBytes("utf-8")
    val bb = ByteBuffer.allocate(3 * 4 + bytes.length + childrenKeysBytes.length + hash.length)
    bb.putInt(bytes.length)
      .putInt(childrenKeysBytes.length)
      .putInt(hash.length)
      .put(bytes)
      .put(childrenKeysBytes)
      .put(hash)
      .array()
  }

  def ===(that: StoreValue): Boolean =
    (bytes sameElements that.bytes) &&
      StoreKeySet.same(childrenKeys, that.childrenKeys) &&
      (hash sameElements that.hash)

  def !==(that: StoreValue): Boolean = ! ===(that)
}

object StoreValue {
  /** deserialize StoreValue from an opaque data
    *
    */
  def deserialize(bytes: Array[Byte]): Option[StoreValue] = {
    scala.util.Try {
      val bb = ByteBuffer.wrap(bytes)
      val bytesLength = bb.getInt
      val childrenKeysLength = bb.getInt
      val hashLength = bb.getInt

      val bytesField = Array.fill(bytesLength)(0.toByte)
      bb.get(bytesField)

      val childrenKeysField = Array.fill(childrenKeysLength)(0.toByte)
      bb.get(childrenKeysField)
      val childrenKeys = StoreKeySet(new String(childrenKeysField, "utf-8").split("\n").map(StoreKey.parse))

      val hashField = Array.fill(hashLength)(0.toByte)
      bb.get(hashField)

      StoreValue(bytesField, childrenKeys, hashField)
    }.toOption
  }
}