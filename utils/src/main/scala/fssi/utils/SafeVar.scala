package fssi.utils

import java.util.concurrent.atomic.AtomicReference

trait SafeVar[A] { outter =>

  private[utils] val a: AtomicReference[Option[A]] = new AtomicReference[Option[A]](None)

  def isDefined: Boolean = a.get().isDefined
  def isEmpty: Boolean   = a.get().isEmpty

  private def _safeUpdate(a1: => A): Unit = {
    if (a.compareAndSet(a.get(), Some(a1))) ()
    else _safeUpdate(a1)
  }

  protected def safeUpdate(a: => A): Unit = _safeUpdate(a)

  def apply(): Option[A]          = a.get()
  def unsafe(): A                 = a.get.get
  def value: A                    = unsafe()
  def toOption: Option[A]         = a.get
  def getOrElse(default: => A): A = toOption.getOrElse(default)

  def :=(a: => A): Unit = safeUpdate(a)

  def foreach(f: A => Unit): Unit = {
    if (a.get().isDefined) f(a.get.get)
    else ()
  }

  def map[B](f: A => B): SafeVar[B] = new SafeVar[B] {
    private[utils] override val a: AtomicReference[Option[B]] =
      new AtomicReference[Option[B]](outter.a.get().map(f))
  }

  def reset(): Unit = a.set(None)

  def updated(f: A => A): this.type = {
    if (a.get().isEmpty) this
    else {
      safeUpdate(f(value))
      this
    }
  }
}

object SafeVar {
  def empty[A]: SafeVar[A] = new SafeVar[A] {}
  def apply[A](a: => A): SafeVar[A] = {
    val i = empty[A]
    i := a
    i
  }
}
