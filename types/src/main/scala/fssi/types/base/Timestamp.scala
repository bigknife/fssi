package fssi
package types
package base

case class Timestamp(value: Long) extends AnyVal

object Timestamp {
  def now: Timestamp = Timestamp(System.currentTimeMillis)
}
