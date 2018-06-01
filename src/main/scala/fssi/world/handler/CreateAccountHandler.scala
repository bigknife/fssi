package fssi.world.handler

import fssi.ast.domain.components.Model
import fssi.ast.usecase.CmdTool
import fssi.interpreter.{Setting, runner}
import fssi.world.Args.CreateAccountArgs
import io.circe.syntax._
import fssi.interpreter.jsonCodec._

class CreateAccountHandler extends ArgsHandler[CreateAccountArgs]{

  val commandTool: CmdTool[Model.Op] = CmdTool[Model.Op]

  override def run(args: CreateAccountArgs): Unit = {
    val p = commandTool.createAccount(args.pass)
    runner.runIOAttempt(p, Setting()).unsafeRunSync() match {
      case Left(t) => t.printStackTrace()
      case Right(account) =>
        println(account.asJson.spaces4)
    }
  }
}
