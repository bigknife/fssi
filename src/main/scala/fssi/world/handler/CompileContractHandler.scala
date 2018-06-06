package fssi.world.handler

import java.nio.file.Paths

import fssi.ast.domain.components.Model
import fssi.ast.usecase.CmdTool
import fssi.interpreter.runner
import fssi.world.Args

class CompileContractHandler extends ArgsHandler[Args.CompileContractArgs] {
  val commandTool: CmdTool[Model.Op] = CmdTool[Model.Op]

  override def run(args: Args.CompileContractArgs): Unit = {
    val p = commandTool.compileContract(Paths.get(args.projectDir))
    runner.runIOAttempt(p, args.toSetting).unsafeRunSync() match {
      case Left(t) => t.printStackTrace()
      case Right(bytesValue) =>
        args.outputFormat match {
          case "jar" =>
            better.files
              .File(args.outputFile)
              .outputStream
              .map(os => os.write(bytesValue.bytes))
              .get()
          case "hex" =>
            better.files
              .File(args.outputFile)
              .outputStream
              .map(os => os.write(bytesValue.hex.getBytes("utf-8")))
              .get()
          case "base64" =>
            better.files
              .File(args.outputFile)
              .outputStream
              .map(os => os.write(bytesValue.base64.getBytes("utf-8")))
              .get()
        }
        println(s"compiled, check output file: ${args.outputFile}")
    }
  }
}
