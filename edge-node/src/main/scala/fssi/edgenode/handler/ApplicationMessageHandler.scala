package fssi.edgenode.handler
import fssi.ast.uc.EdgeNodeProgram
import fssi.interpreter.Setting.EdgeNodeSetting
import fssi.types.ApplicationMessage
import fssi.types.biz.Message
import fssi.interpreter._

trait ApplicationMessageHandler extends LogSupport {

  private lazy val edgeNodeProgram = EdgeNodeProgram.instance

  private val applicationMessageHandler
    : EdgeNodeSetting => Message.Handler[ApplicationMessage, Unit] =
    edgeNodeSetting =>
      Message.handler[ApplicationMessage, Unit] { applicationMessage =>
        val program = edgeNodeProgram.processApplicationMessage(applicationMessage)
        runner.runIOAttempt(program, edgeNodeSetting).unsafeRunSync() match {
          case Right(_) =>
            log.info(s"edge node handle application message success")
          case Left(e) =>
            log.error(s"edge node handle application message failed: ${e.getMessage}", Some(e))
        }
    }

  def apply(edgeNodeSetting: EdgeNodeSetting): Message.Handler[ApplicationMessage, Unit] =
    applicationMessageHandler(edgeNodeSetting)
}
