package fssi.store.mpt

sealed trait Path {
  def segments: Array[Byte]
  def encoded: Array[Byte]
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
}
