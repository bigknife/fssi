package fssi.store.mpt

import java.nio.ByteBuffer

sealed trait Data {
  def bytes: Array[Byte]

  val hash: Hash = Hash.encode(bytes)
  val length: Int = bytes.length

  def toNode: Option[Node] = Data.decode(this)
}
object Data {
  def empty: Data = new Data {
    override def bytes: Array[Byte] = Array.emptyByteArray
  }

  def wrap(v: Array[Byte]): Data = new Data {
    override def bytes: Array[Byte] = v
  }

  def encode[N <: Node](node: N): Data = node match {
    case Node.Null => Data.empty
    case _@Node.Leaf(path, data) =>
      // 0x01 + Int(path.length) + Int(data.length) + path.bytes + data.bytes
      val buf = ByteBuffer.allocate(1 + 4 + 4 + path.length + data.length)
      //head
      buf.put(0x01.toByte)
      buf.putInt(path.length)
      buf.putInt(data.length)
      // body
      buf.put(path.segments)
      buf.put(data.bytes)
      Data.wrap(buf.array())

    case _@Node.Extension(path, key) =>
      // 0x02 + Int(path.length) + Int(key.length) + path.bytes + key.bytes
      val buf = ByteBuffer.allocate(1 + 4 + 4 + path.length + key.length)
      //head
      buf.put(0x02.toByte)
      buf.putInt(path.length)
      buf.putInt(key.length)
      //body
      buf.put(path.segments)
      buf.put(key.bytes)
      Data.wrap(buf.array())

    case _@Node.Branch(slot, data) =>
      // 0x03 + Int(slot.length) + Int(data.length) + slot.bytes + data.bytes
      val slotBytes = Slot.Hex.encode(slot)
      val buf = ByteBuffer.allocate(1 + 4 + 4 + slotBytes.length + data.length)
      buf.put(0x03.toByte)
      buf.putInt(slotBytes.length)
      buf.putInt(data.length)
      buf.put(slotBytes)
      buf.put(data.bytes)
      Data.wrap(buf.array())
  }
  def decode(data: Data): Option[Node] = ???
}
