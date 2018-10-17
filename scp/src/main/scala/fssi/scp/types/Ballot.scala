package fssi.scp.types

case class Ballot(counter: Int, value: Value) extends Ordered[Ballot] {
  def compare(b: Ballot): Int = {
    val c = Ordering[Int].compare(counter, b.counter)
    if (c == 0) value.compare(b.value)
    else c
  }
}
