package fssi.world.handler

import bigknife.cli.Program
import fssi.world.Args

trait ArgsHandler[A <: Args] {

  def logbackConfigResource(args: A): String

  private def initLogback(args: A): Unit = {
    val conf = logbackConfigResource(args)
    val is = getClass.getResourceAsStream(conf)
    try {
      LogbackUtil.setConfig(is)
    } finally {
      is.close()
    }
    ()
  }

  // init logback
  // initLogback()

  def run(args: A): Unit

  def runWorld(args: A): Unit = {
    initLogback(args)
    run(args)
  }
}

object ArgsHandler {
  def apply[A <: Args](implicit AH: ArgsHandler[A]): ArgsHandler[A] = AH

  def unsupported: ArgsHandler[Args] = new ArgsHandler[Args] {
    override def logbackConfigResource(args: Args): String = "/logback.xml"
    override def run(args: Args): Unit = ()
  }

  trait Implicits {
    implicit lazy val emptyArgsHandler: ArgsHandler[Args.EmptyArgs] = EmptyArgsHandler
    implicit lazy val nymphArgsHandler: ArgsHandler[Args.NymphArgs] = new NymphHandler
    implicit lazy val createAccountArgsHandler: ArgsHandler[Args.CreateAccountArgs] =
      new CreateAccountHandler
    implicit lazy val warriorHandler: ArgsHandler[Args.WarriorArgs] = new WarriorHandler

    /*
    implicit lazy val argsHandler: ArgsHandler[Args] = summon {
      case a: Args.EmptyArgs         => ArgsHandler[Args.EmptyArgs].run(a)
      case a: Args.NymphArgs         => ArgsHandler[Args.NymphArgs].run(a)
      case a: Args.CreateAccountArgs => ArgsHandler[Args.CreateAccountArgs].run(a)
      case a: Args.WarriorArgs       => ArgsHandler[Args.WarriorArgs].run(a)
      case _                         => unsupported.run(Args.default)
    }*/

  }
  object implicits extends Implicits

  import ArgsHandler.implicits._
  lazy implicit val program: Program[Args] = {
    case a: Args.EmptyArgs         => ArgsHandler[Args.EmptyArgs].runWorld(a)
    case a: Args.CreateAccountArgs => ArgsHandler[Args.CreateAccountArgs].runWorld(a)
    case a: Args.NymphArgs         => ArgsHandler[Args.NymphArgs].runWorld(a)
    case a: Args.WarriorArgs       => ArgsHandler[Args.WarriorArgs].runWorld(a)
  }
}
