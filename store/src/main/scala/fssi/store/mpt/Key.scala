package fssi.store.mpt



sealed trait Key {
  def bytes: Array[Byte]

  def isEmpty: Boolean = bytes.length == 0
  def nonEmpty: Boolean = bytes.length != 0

  def toPath: Path = Path.PlainPath(bytes)

  val length = bytes.length
}

object Key {
  def empty: Key = new Key {
    override def bytes: Array[Byte] = Array.emptyByteArray
  }
  def encode(hash: Hash): Key = new Key {
    override val bytes: Array[Byte] =
      hash.bytes.map("%02x" format _).mkString("").getBytes("utf-8")
  }
  def wrap(v: Array[Byte]): Key = new Key {
    override def bytes: Array[Byte] = v
  }
}