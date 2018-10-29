package fssi
package types
package base

case class Timestamp(value: Long) extends AnyVal {
  def ===(other: Timestamp): Boolean = value == other.value
}

object Timestamp {
  def now: Timestamp = Timestamp(System.currentTimeMillis)
}
