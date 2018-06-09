package fssi.world.handler

import bigknife.jsonrpc.Request
import fssi.ast.domain.components.Model
import fssi.ast.usecase.CmdTool
import fssi.interpreter.runner
import fssi.world.Args
import io.circe.syntax._
import bigknife.jsonrpc.implicits._
import fssi.interpreter.jsonCodec._

class CreateRunContractHandler extends ArgsHandler[Args.CreateRunContractArgs] {
  val commandTool: CmdTool[Model.Op] = CmdTool[Model.Op]

  override def run(args: Args.CreateRunContractArgs): Unit = {
    val p = commandTool.createRunContract(args.accountId,
                                          args.privateKey,
                                          args.password,
                                          args.iv,
                                          args.name,
                                          args.version,
                                          args.function,
                                          args.params)
    runner.runIOAttempt(p, args.toSetting).unsafeRunSync() match {
      case Left(t)                => t.printStackTrace()
      case Right(publishContract) =>
        // to jsonrpc protocol
        val jsonrpc = Request(
          publishContract.id.value,
          "sendTransaction",
          publishContract
        )
        args.outputFormat match {
          case "space2" => println(jsonrpc.asJson.spaces2)
          case "space4" => println(jsonrpc.asJson.spaces4)
          case _        => println(jsonrpc.asJson.noSpaces)
        }

    }
  }
}
