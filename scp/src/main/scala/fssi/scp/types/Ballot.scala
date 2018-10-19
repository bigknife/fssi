package fssi.scp.types

sealed trait Ballot extends Ordered[Ballot] {
  def counter: Int
  def value: Value
}

object Ballot {
  private case object Bottom extends Ballot {
    override def counter: Int = 0

    override def value: Value = throw new RuntimeException("Bottom Ballot Has No Value")

    override def compare(that: Ballot): Int = that match {
      case Bottom => 0
      case _      => -1
    }
  }

  private case class CommonBallot(counter: Int, value: Value) extends Ballot {
    override def compare(that: Ballot): Int = that match {
      case Bottom => 1
      case CommonBallot(thatCounter, thatValue) =>
        val c = Ordering[Int].compare(counter, thatCounter)
        if (c == 0) value.compare(thatValue)
        else c
    }
  }

  def bottom: Ballot = Bottom
  def zero: Ballot   = Bottom

  def apply(counter: Int, value: Value): Ballot = CommonBallot(counter, value)
  def max(value: Value): Ballot = CommonBallot(Int.MaxValue, value)
}
