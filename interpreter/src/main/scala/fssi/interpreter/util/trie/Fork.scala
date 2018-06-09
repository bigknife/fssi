package fssi.interpreter.util.trie

/** define a fixed, immutable array
  *
  * @tparam A the element of type A
  */
sealed trait Fork[A] {
  protected[Fork] val data: Vector[Option[A]]

  def apply(i: Int): Option[A] = {
    val idx    = i % data.length
    val posIdx = if (idx < 0) data.length + idx else idx
    data(posIdx)
  }

  def set(i: Int, a: A): Fork[A] = {
    val idx    = i % data.length
    val posIdx = if (idx < 0) data.length + idx else idx
    val newData = data.foldLeft((0, Vector.empty[Option[A]])) {
      case ((i0, vec), _) if i0 == posIdx => (i0 + 1, vec :+ Some(a))
      case ((i0, vec), n)                 => (i0 + 1, vec :+ n)
    }
    new Fork[A] {
      val data: Vector[Option[A]] = newData._2
    }
  }

  def map[B](f: A => B): Fork[B] = {
    val d1 = data.map(x => x.map(f))
    new Fork[B] {
      val data: Vector[Option[B]] = d1
    }
  }
}

object Fork {
  trait Fork16[A] { out =>
    protected[Fork16] val fork = `16`[A]

    def apply(i: Int): Option[A] = fork(i)
    def set(i: Int, a: A): Fork16[A] = {
      val inner = this.fork.set(i, a)
      new Fork16[A] {
        override protected[Fork16] val fork: Fork[A] = inner
      }
    }
    def map[B](f: A => B): Fork16[B] = {
      val inner = this.fork.map(f)
      new Fork16[B]{
        override protected[Fork16] val fork: Fork[B] = inner
      }
    }
    def data: Vector[Option[A]] = fork.data
  }
  def fork16[A]: Fork16[A] = new Fork16[A] {}

  def `16`[A]: Fork[A] = new Fork[A] {
    val data: Vector[Option[A]] = Vector(
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
    )
  }

}
