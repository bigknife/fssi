package fssi.edgenode.handler
import fssi.ast.uc.EdgeNodeProgram
import fssi.interpreter.LogSupport
import fssi.interpreter.Setting.EdgeNodeSetting
import fssi.types.ClientMessage
import fssi.types.biz.{Message, Transaction}
import fssi.interpreter._

trait ClientMessageHandler extends LogSupport {

  private lazy val edgeNodeProgram = EdgeNodeProgram.instance

  private val clientMessageHandler: EdgeNodeSetting => Message.Handler[ClientMessage, Transaction] =
    edgeNodeSetting =>
      Message.handler[ClientMessage, Transaction] { clientMessage =>
        val program = edgeNodeProgram.processClientMessage(clientMessage)
        runner.runIOAttempt(program, edgeNodeSetting).unsafeRunSync() match {
          case Right(transaction) =>
            log.info(s"edge node handle client message success")
            transaction
          case Left(e) =>
            log.error(s"edge node handle client message failed: ${e.getMessage}", Some(e))
            throw e
        }
    }

  def apply(edgeNodeSetting: EdgeNodeSetting): Message.Handler[ClientMessage, Transaction] =
    clientMessageHandler(edgeNodeSetting)
}
