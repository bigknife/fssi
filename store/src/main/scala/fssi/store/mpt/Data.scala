package fssi.store.mpt

import java.nio.ByteBuffer

sealed trait Data {
  def bytes: Array[Byte]

  val hash: Hash = Hash.encode(bytes)
  val length: Int = bytes.length
  def isEmpty: Boolean = bytes.isEmpty

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
  def decode(data: Data): Option[Node] = {
    if (data.isEmpty) Some(Node.Null)
    else {
      val buf = ByteBuffer.wrap(data.bytes)

      val flag = Array(0.toByte)
      buf.get(flag)
      flag(0) match {
        case 0x01 => // leaf
          val pathLength = buf.getInt
          val dataLength = buf.getInt
          val pathBytes = Array.fill(pathLength)(0.toByte)
          buf.get(pathBytes)
          val dataBytes = Array.fill(dataLength)(0.toByte)
          buf.get(dataBytes)
          Some(Node.leaf(Path.plain(pathBytes), Data.wrap(dataBytes)))

        case 0x02 => // extension
          val pathLength = buf.getInt
          val keyLength = buf.getInt
          val pathBytes = Array.fill(pathLength)(0.toByte)
          buf.get(pathBytes)
          val keyBytes = Array.fill(keyLength)(0.toByte)
          buf.get(keyBytes)
          Some(Node.extension(Path.plain(pathBytes), Key.wrap(keyBytes)))

        case 0x03 => // branch
          val slotLength = buf.getInt
          val dataLength = buf.getInt
          val slotBytes = Array.fill(slotLength)(0.toByte)
          buf.get(slotBytes)
          val dataBytes = Array.fill(dataLength)(0.toByte)
          buf.get(dataBytes)
          for {
            slot <- Slot.Hex.decode(slotBytes)
          } yield Node.branch(slot, Data.wrap(dataBytes))

        case _ => None
      }
    }
  }
}
