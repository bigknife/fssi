package fssi.world.handler

import fssi.world.Args

object EmptyArgsHandler extends ArgsHandler[Args.EmptyArgs] {
  override def run(args: Args.EmptyArgs): Unit = println("nothing to do")

  override def logbackConfigResource(args: Args.EmptyArgs): String = "/logback.xml"
}
