package fssi.world.handler

import fssi.world.Args

trait ArgsHandler[A <: Args] {
  def run(args: A): Unit
}

object ArgsHandler {
  def apply[A <: Args](implicit AH: ArgsHandler[A]): ArgsHandler[A] = AH

  def summon[A <: Args](f: A => Unit): ArgsHandler[A] = new ArgsHandler[A] {
    def run(args: A): Unit = f(args)
  }

  trait Implicits {
    implicit lazy val emptyArgsHandler: ArgsHandler[Args.EmptyArgs] = EmptyArgsHandler

    implicit lazy val argsHandler: ArgsHandler[Args] = summon {
      case a: Args.EmptyArgs => ArgsHandler[Args.EmptyArgs].run(a)
      case _ => ???
    }

  }
  object implicits extends Implicits
}
