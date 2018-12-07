package fssi
package types
package base

case class Timestamp(value: Long) extends AnyVal {
  def ===(other: Timestamp): Boolean = value == other.value
}

object Timestamp {
  def now: Timestamp = Timestamp(System.currentTimeMillis)

  trait Implicits {
    implicit def timestampToArrayBytes(ts: Timestamp): Array[Byte] =
      BigInt.apply(ts.value).toByteArray
  }
}
