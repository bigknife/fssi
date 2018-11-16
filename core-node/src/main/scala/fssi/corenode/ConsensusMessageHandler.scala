package fssi.corenode

import fssi.ast.uc.CoreNodeProgram
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.types.biz.Message
import fssi.types.biz.Message.ConsensusMessage
import fssi.interpreter._
import fssi.types.implicits._

trait ConsensusMessageHandler extends LogSupport {

  private lazy val coreNodeProgram = CoreNodeProgram.instance

  private val handler: CoreNodeSetting => Message.Handler[ConsensusMessage, Unit] =
    coreNodeSetting =>
      Message.handler[ConsensusMessage, Unit] { consensusMessage =>
        val program = coreNodeProgram.processConsensusMessage(consensusMessage)
        runner.runIOAttempt(program, coreNodeSetting).unsafeRunSync() match {
          case Right(_) =>
            log.info(s"core node handle consensus message success")
          case Left(e) =>
            log.error(s"core node handle consensus message failed: ${e.getMessage}", Some(e))
        }
    }

  def apply(coreNodeSetting: CoreNodeSetting): Message.Handler[ConsensusMessage, Unit] =
    handler(coreNodeSetting)
}
