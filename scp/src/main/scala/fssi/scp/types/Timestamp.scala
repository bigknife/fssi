package fssi.scp.types

case class Timestamp(value: Long) extends AnyVal

object Timestamp {
  trait Implicits {
    import fssi.base.implicits._
    implicit def timestampToBytes(timestamp: Timestamp): Array[Byte] =
      timestamp.value.asBytesValue.bytes
  }
}
