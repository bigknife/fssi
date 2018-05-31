package fssi.world.handler

import fssi.world.Args

trait ArgsHandler[A <: Args] {
  def run(args: A): Unit
}

object ArgsHandler {
  def apply[A <: Args](implicit AH: ArgsHandler[A]): ArgsHandler[A] = AH

  def summon[A <: Args](f: A => Unit): ArgsHandler[A] = (args: A) => f(args)

  trait Implicits {
    implicit lazy val emptyArgsHandler: ArgsHandler[Args.EmptyArgs] = EmptyArgsHandler
    implicit lazy val nymphArgsHandler: ArgsHandler[Args.NymphArgs] = new NymphHandler

    implicit lazy val argsHandler: ArgsHandler[Args] = summon {
      case a: Args.EmptyArgs => ArgsHandler[Args.EmptyArgs].run(a)
      case a: Args.NymphArgs => ArgsHandler[Args.NymphArgs].run(a)
      case _ => ???
    }

  }
  object implicits extends Implicits
}
