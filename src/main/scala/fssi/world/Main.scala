package fssi.world

import bigknife.cli.{CommandLineProgram, Program}
import fssi.ast.domain.components.Model
import fssi.ast.usecase.Nymph
import fssi.world.handler.ArgsHandler

object Main{
  import bigknife.cli.scopt._
  import fssi.world.Args.implicits._

  lazy val nymph: Nymph[Model.Op]          = Nymph[Model.Op]
  lazy implicit val model: Model[Model.Op] = Model[Model.Op]

  // this is where the real app runs
  import ArgsHandler.implicits._
  lazy implicit val p: Program[Args] = ArgsHandler[Args].run(_)

  def main(args: Array[String]): Unit = {
    CommandLineProgram[Args](args, Args.default).run()
  }
}
