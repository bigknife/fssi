package fssi.base

import java.util.concurrent.atomic.AtomicReference

sealed trait Var[A] {
  def apply(a: A): this.type
  def apply(): Option[A]
  def unsafe(): A = apply().get

  def :=(a: A): this.type = apply(a)

  def map[B](f: A => B): Var[B] =
    apply().map(f).map(x => Var[B](x)).getOrElse(Var.empty[B])

  def flatMap[B](f: A => Var[B]): Var[B] =
    apply().map(f).getOrElse(Var.empty[B])

  def exists(f: A => Boolean): Boolean = {
    apply().exists(f)
  }

  def foreach(f: A => Unit): Unit =
    apply().foreach(f)

  def getOrElse(default: => A): A = apply().getOrElse(default)
  def update(f: A => A): this.type = {
    apply().map(f) match {
      case Some(newA) => apply(newA)
      case _ => this
    }
  }

  def isEmpty: Boolean   = apply().isEmpty
  def nonEmpty: Boolean  = apply().nonEmpty
  def isDefined: Boolean = apply().isDefined
  def clear: Unit

}

object Var {
  def empty[A]: Var[A]       = new SimpleVar[A]
  def apply[A](a: A): Var[A] = new SimpleVar[A]()(a)

  private[Var] class SimpleVar[A]() extends Var[A] {
    private val container: AtomicReference[A] = new AtomicReference[A]()

    override def apply(a: A): SimpleVar.this.type = {
      def _loop(a: A): SimpleVar.this.type =
        if (container.compareAndSet(container.get(), a)) this
        else _loop(a)

      _loop(a)
    }

    override def apply(): Option[A] = Option(container.get())

    override def toString: String = {
      apply().map(_.toString).getOrElse("null")
    }

    override def clear: Unit = container.set(null.asInstanceOf[A])
  }
}
