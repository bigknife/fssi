package fssi.interpreter.util

trait SafeVar[A] {outter =>
  @volatile
  private var a: Option[A] = None

  def isDefined: Boolean = a.isDefined
  def isEmpty: Boolean = a.isEmpty

  private def safeUpdate(a: => A): Unit = synchronized {
    unsafeUpdate(a)
  }

  protected def unsafeUpdate(a: => A): Unit = outter.a = Some(a)

  def apply(): Option[A] = a
  def unsafe(): A = a.get

  def := (a: => A): Unit = safeUpdate(a)

  def foreach(f: A => Unit): Unit = {
    if (a.isDefined) f(a.get)
    else ()
  }

  def reset(): Unit = a = None
}

