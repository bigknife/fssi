package fssi.scp
package interpreter

import java.util.{Timer, TimerTask}

import bigknife.sop._
import fssi.scp.ast._
import fssi.scp.interpreter.store._
import fssi.scp.types._

import scala.util.Try

class ApplicationServiceHandler extends ApplicationService.Handler[Stack] with LogSupport {

  private val timerCache: Var[Map[String, Vector[Timer]]] = Var(Map.empty)

  /** validate value on application level
    */
  override def validateValue(nodeId: NodeID,
                             slotIndex: SlotIndex,
                             value: Value): Stack[Value.Validity] = Stack { setting =>
    setting.applicationCallback.validateValue(nodeId, slotIndex, value)
  }

  /** validate some values
    */
  override def validateValues(nodeId: NodeID,
                              slotIndex: SlotIndex,
                              values: ValueSet): Stack[Value.Validity] = Stack { setting =>
    import Value.Validity._
    //(result, everInvalid, everFullValidated, everMaybeValid)
    val fullValidatedCount = values.count(
      setting.applicationCallback.validateValue(nodeId, slotIndex, _) == FullyValidated)
    if (fullValidatedCount == values.size) FullyValidated
    else if (values.exists(
               setting.applicationCallback.validateValue(nodeId, slotIndex, _) == MaybeValid))
      MaybeValid
    else Invalid
  }

  /** combine values to ONE value, maybe nothing
    */
  override def combineCandidates(nodeId: NodeID,
                                 slotIndex: SlotIndex,
                                 xs: ValueSet): Stack[Option[Value]] = Stack { setting =>
    setting.applicationCallback.combineValues(nodeId, slotIndex, xs)
  }

  /** extract valida value from a not fully validated value
    */
  override def extractValidValue(nodeId: NodeID,
                                 slotIndex: SlotIndex,
                                 value: Value): Stack[Option[Value]] = Stack { setting =>
    setting.applicationCallback.extractValidValue(nodeId, slotIndex, value)
  }

  /** after timeout milliseconds, execute the program
    *
    * @param tag the delay timer tag, we can cancel the timer by using this tag later.
    * @param program with type: SP[F, Unit]
    */
  override def delayExecuteProgram(tag: String, program: Any, timeout: Long): Stack[Unit] = Stack {
    setting =>
      val task = new Runnable {
        override def run(): Unit = {
          runner
            .runIOAttempt(program.asInstanceOf[SP[components.Model.Op, Unit]], setting)
            .unsafeRunSync() match {
            case Left(t) =>
              log.error("delayed program execution failed", t)
            case Right(_) =>
              log.debug("delayed program execution success")
          }
        }
      }

      if (setting.applicationCallback.canDispatch)
        setting.applicationCallback.dispatch(tag, timeout, task)
      else {
        val timer = new Timer()

        timerCache.update { m =>
          if (m.contains(tag)) m + (tag -> (m(tag) :+ timer))
          else m + (tag                 -> Vector(timer))
        }
        timer.schedule(new TimerTask {
          override def run(): Unit = try {
            SCPThreadPool.submit(task)
          } finally timer.cancel()
        }, timeout)
      }
  }

  /** cancel the timer
    */
  override def stopDelayTimer(tag: String): Stack[Unit] = Stack { setting =>
    if (setting.applicationCallback.canDispatch) {
      setting.applicationCallback.cancel(tag)
    } else {
      timerCache.foreach { m =>
        if (m.contains(tag)) {
          m(tag).foreach(_.cancel())
          timerCache.update(_ - tag)
        } else ()
      }
    }
  }

  /** listener, phase upgrade to confirm
    */
  override def phaseUpgradeToConfirm(slotIndex: SlotIndex, ballot: Ballot): Stack[Unit] = Stack {
    setting =>
      setting.applicationCallback.valueConfirmed(slotIndex, ballot.value)
  }

  /** listener, phase upgrade to externalize
    */
  override def phaseUpgradeToExternalize(slotIndex: SlotIndex, ballot: Ballot): Stack[Unit] =
    Stack { setting =>
      setting.applicationCallback.valueExternalized(slotIndex, ballot.value)
    }

  /** broadcast message envelope
    */
  override def broadcastEnvelope[M <: Message](slotIndex: SlotIndex,
                                               envelope: Envelope[M]): Stack[Unit] = Stack {
    setting =>
      envelope.statement.message match {
        case x: Message.Nomination =>
          val nominationStatus = NominationStatus.getInstance(slotIndex)

          nominationStatus.lastEnvelope := Some(envelope.to[Message.Nomination])
          setting.applicationCallback.broadcastEnvelope(slotIndex, envelope)

        case _: Message.BallotMessage =>
          sendLatestBallotEnvelope(setting, slotIndex)
        case _ =>
      }
  }

  override def sendLatestEnvelope(slotIndex: SlotIndex): Stack[Unit] = Stack { setting =>
    sendLatestBallotEnvelope(setting, slotIndex)
  }

  private def sendLatestBallotEnvelope(setting: Setting, slotIndex: SlotIndex): Unit = {
    val ballotStatus = BallotStatus.getInstance(slotIndex)

    if (ballotStatus.currentMessageLevel
          .unsafe() == 0 && ballotStatus.latestGeneratedEnvelope.isDefined && setting.applicationCallback.isValidator) {
      if (!ballotStatus.latestEmitEnvelope
            .exists(_.contains(ballotStatus.latestGeneratedEnvelope))) {
        ballotStatus.latestEmitEnvelope := Some(ballotStatus.latestGeneratedEnvelope.unsafe())
        setting.applicationCallback
          .broadcastEnvelope(slotIndex, ballotStatus.latestGeneratedEnvelope.unsafe())
      }
    }
  }
}

object ApplicationServiceHandler {
  val instance = new ApplicationServiceHandler

  trait Implicits {
    implicit val scpApplicationServiceHandler: ApplicationServiceHandler = instance
  }
}
