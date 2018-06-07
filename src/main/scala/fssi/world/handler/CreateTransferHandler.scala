package fssi.world.handler

import bigknife.jsonrpc.Request
import fssi.ast.domain.components.Model
import fssi.ast.usecase.CmdTool
import fssi.interpreter.runner
import fssi.world.Args
import io.circe.syntax._
import bigknife.jsonrpc.implicits._
import fssi.interpreter.jsonCodec._

/**
  * create transfer handler
  */
class CreateTransferHandler extends ArgsHandler[Args.CreateTransferArgs] {
  val commandTool: CmdTool[Model.Op] = CmdTool[Model.Op]

  override def logbackConfigResource(args: Args.CreateTransferArgs): String = "/logback.xml"

  override def run(args: Args.CreateTransferArgs): Unit = {
    val p = commandTool.createTransfer(args.accountId,
                                       args.transferTo,
                                       args.amount,
                                       args.privateKey,
                                       args.password,
                                       args.iv)
    runner.runIOAttempt(p, args.toSetting).unsafeRunSync() match {
      case Left(t)         => t.printStackTrace()
      case Right(transfer) =>
        // to jsonrpc protocol
        val jsonrpc = Request(
          transfer.id.value,
          "sendTransaction",
          transfer
        )
        args.outputFormat match {
          case "space2" => println(jsonrpc.asJson.spaces2)
          case "space4" => println(jsonrpc.asJson.spaces4)
          case _        => println(jsonrpc.asJson.noSpaces)
        }

    }
  }
}
