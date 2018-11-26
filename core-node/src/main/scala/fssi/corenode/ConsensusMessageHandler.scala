package fssi.corenode

import fssi.ast.uc.CoreNodeProgram
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.types.biz.Message
import fssi.types.biz.Message.ConsensusMessage
import fssi.interpreter._
import fssi.interpreter.scp.SCPEnvelope

trait ConsensusMessageHandler extends LogSupport {

  private lazy val coreNodeProgram = CoreNodeProgram.instance

  private val handler: CoreNodeSetting => Message.Handler[ConsensusMessage, Unit] =
    coreNodeSetting =>
      Message.handler[ConsensusMessage, Unit] {
        case scpEnvelope: SCPEnvelope =>
          val program = coreNodeProgram.processConsensusMessage(scpEnvelope)
          runner.runIOAttempt(program, coreNodeSetting).unsafeRunSync() match {
            case Right(_) =>
              log.debug(
                s"core node handle consensus message from node [${scpEnvelope.value.statement.from}] success")
            case Left(e) =>
              log.error(
                s"core node handle consensus message from node [${scpEnvelope.value.statement.from}] failed: ${e.getMessage}",
                Some(e))
              throw e
          }
        case x => throw new RuntimeException(s"unsupported consensus message: $x")
    }

  def apply(coreNodeSetting: CoreNodeSetting): Message.Handler[ConsensusMessage, Unit] =
    handler(coreNodeSetting)
}
