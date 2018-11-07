package fssi.store.mpt

sealed trait Path {
  def segments: Array[Byte]
  def encoded: Array[Byte]

  def ===(that: Path): Boolean = this.segments sameElements that.segments
  def startWith(that: Path): Boolean = this.segments.startsWith(that.segments)

  def drop(that: Path): Path = {
    if (startWith(that)) Path.PlainPath(segments.drop(that.segments.length))
    else Path.PlainPath(segments)
  }
  def dropHead: Path = Path.PlainPath(segments.drop(1))

  def isEmpty: Boolean = segments.isEmpty
  def nonEmpty: Boolean = segments.nonEmpty
  def head: Byte = segments.head
  def prefix(that: Path): Path = {
    val min = scala.math.min(segments.length, that.segments.length)

    def _loop(acc: Array[Byte], i: Int, max: Int, bytes1: Array[Byte], bytes2: Array[Byte]): Array[Byte] = {
      if (i == max) acc
      else if (bytes1(i) == bytes2(i)) _loop(acc :+ bytes1(i), i + 1, max, bytes1, bytes2)
      else acc
    }

    Path.PlainPath(_loop(Array.emptyByteArray, 0, min, segments, that.segments))
  }
  def hasPrefix(that: Path): Boolean = this.segments(0) == that.segments(0)
  def length: Int = segments.length

  def asExtensionPath: Path.ExtensionPath = Path.ExtensionPath(segments)
  def asLeafPath: Path.LeafPath = Path.LeafPath(segments)
  def asPlainPath: Path.PlainPath = Path.PlainPath(segments)

  override def toString: String = new String(segments, "utf-8")
}

object Path {
  case class LeafPath(segments: Array[Byte]) extends Path {
    override def encoded: Array[Byte] = ???
  }
  case class ExtensionPath(segments: Array[Byte]) extends Path {
    override def encoded: Array[Byte] = ???
  }
  case class PlainPath(segments: Array[Byte]) extends Path {
    override def encoded: Array[Byte] = segments
  }

  def leaf(x: Array[Byte]): Path = LeafPath(x)
  def extension(x: Array[Byte]): Path = ExtensionPath(x)
  def plain(x: Array[Byte]) = PlainPath(x)
}
