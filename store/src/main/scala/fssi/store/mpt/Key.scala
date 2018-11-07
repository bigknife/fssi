package fssi.store.mpt

import org.bouncycastle.pqc.math.linearalgebra.ByteUtils


sealed trait Key {
  def bytes: Array[Byte]

  def isEmpty: Boolean = bytes.length == 0
  def nonEmpty: Boolean = bytes.length != 0

  def toPath: Path = Path.PlainPath(bytes)

  val length = bytes.length

  override def toString: String = {
    if (bytes.isEmpty) "Key(Null)"
    else s"Key(${new String(bytes, "utf-8")})"
  }
}

object Key {
  def empty: Key = new Key {
    override def bytes: Array[Byte] = Array.emptyByteArray
  }
  def encode(hash: Hash): Key = new Key {
    override def bytes: Array[Byte] = {
      val str = hash.bytes.map("%02x" format _).mkString("")
      str.getBytes("utf-8")
    }
  }
  def decode(key: Key): Hash = Hash.wrap(
    ByteUtils.fromHexString(new String(key.bytes, "utf-8"))
  )

  def wrap(v: Array[Byte]): Key = new Key {
    override def bytes: Array[Byte] = v
  }
}