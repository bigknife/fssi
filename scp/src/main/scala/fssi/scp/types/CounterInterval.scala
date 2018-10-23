package fssi.scp.types

sealed trait CounterInterval {
  def first: Int
  def second: Int

  def withFirst(first: Int): CounterInterval
  def withSecond(second: Int): CounterInterval

  def isAvailable: Boolean  = first > 0
  def notAvailable: Boolean = first <= 0
}

object CounterInterval {
  private case class SimpleCounterInterval(first: Int, second: Int) extends CounterInterval {
    def withFirst(first: Int): CounterInterval =
      if (first < 0) this
      else this.copy(first = first)

    def withSecond(second: Int): CounterInterval =
      if (second < first) this
      else this.copy(second = second)
  }

  def apply(x: Int = 0): CounterInterval = SimpleCounterInterval(x, x)
  def apply(x: Int, y: Int): CounterInterval = {
    val f = if (x <= 0) 0 else x
    val s = if (y <= 0) 0 else y
    SimpleCounterInterval(f, s)
  }
}
