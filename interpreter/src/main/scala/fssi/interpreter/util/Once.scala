package fssi.interpreter.util

sealed trait Once[A] extends SafeVar[A] {
  override protected def unsafeUpdate(a: => A): Unit = {
    if (isEmpty) super.unsafeUpdate(a)
    else ()
  }
}

object Once {
  def apply[A](a: => A): Once[A] = {
    val once = new Once[A] {}
    once := a
    once
  }

  def empty[A]: Once[A] = new Once[A] {}
}
