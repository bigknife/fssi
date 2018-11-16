package fssi.corenode

import fssi.ast.uc.CoreNodeProgram
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.types.ApplicationMessage
import fssi.types.biz.Message
import fssi.interpreter._

trait ApplicationMessageHandler extends LogSupport {

  private lazy val coreNodeProgram = CoreNodeProgram.instance

  private val handler: CoreNodeSetting => Message.Handler[ApplicationMessage, Unit] =
    coreNodeSetting =>
      Message.handler[ApplicationMessage, Unit] { applicationMessage =>
        val program = coreNodeProgram.processApplicationMessage(applicationMessage)
        runner.runIOAttempt(program, coreNodeSetting).unsafeRunSync() match {
          case Right(_) =>
            log.info(s"core node handle application message success")
          case Left(e) =>
            log.error(s"core node handle application message failed: ${e.getMessage}", Some(e))
        }
    }

  def apply(coreNodeSetting: CoreNodeSetting): Message.Handler[Message.ApplicationMessage, Unit] =
    handler(coreNodeSetting)
}
