package fssi.scp.types

sealed trait Ballot extends Ordered[Ballot] {
  def counter: Int
  def value: Value
  def isLess(other: Ballot): Boolean = counter <= other.counter
  def isBottom: Boolean

  def compatible(b2: Ballot): Boolean   = value == b2.value
  def incompatible(b2: Ballot): Boolean = value != b2.value
}

object Ballot {
  private case object Bottom extends Ballot {
    override def counter: Int = 0

    override def value: Value = throw new RuntimeException("Bottom Ballot Has No Value")

    override def compare(that: Ballot): Int = that match {
      case Bottom => 0
      case _      => -1
    }
    override def isBottom: Boolean = true
  }

  private case class CommonBallot(counter: Int, value: Value) extends Ballot {
    override def compare(that: Ballot): Int = that match {
      case Bottom => 1
      case CommonBallot(thatCounter, thatValue) =>
        val c = Ordering[Int].compare(counter, thatCounter)
        if (c == 0) value.compare(thatValue)
        else c
    }
    override def isBottom: Boolean = false
  }

  def bottom: Ballot = Bottom
  def zero: Ballot   = Bottom

  def apply(counter: Int, value: Value): Ballot = CommonBallot(counter, value)
  def max(value: Value): Ballot                 = CommonBallot(Int.MaxValue, value)

  sealed trait Phase
  object Phase {
    case object Prepare     extends Phase
    case object Confirm     extends Phase
    case object Externalize extends Phase
  }
}
