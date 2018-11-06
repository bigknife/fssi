package fssi.store.mpt

sealed trait Data {
  def bytes: Array[Byte]

  val hash: Hash = Hash.encode(bytes)

  def toNode: Option[Node] = ???
}
object Data {
  def empty: Data = new Data {
    override def bytes: Array[Byte] = Array.emptyByteArray
  }

  def wrap(v: Array[Byte]): Data = new Data {
    override def bytes: Array[Byte] = v
  }

  def encode[N <: Node](node: N): Data = ???
}
