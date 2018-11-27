package fssi
package scp
package interpreter

import fssi.scp.ast._
import fssi.scp.interpreter.store.{BallotStatus, NominationStatus}
import fssi.scp.types.{Envelope, Message}
import org.slf4j.{Logger, LoggerFactory}
import fssi.scp.types.implicits._
import fssi.base.BytesValue.implicits._

/** log service, use logback as the backend
  *
  */
class LogServiceHandler extends LogService.Handler[Stack] {
  // all ast logs use the same name: fssi.ast
  private val logger = LoggerFactory.getLogger("fssi.scp.ast")

  private lazy val envelopeReceivedLogger      = LoggerFactory.getLogger("fssi.scp.ast.envelope.recv")
  private lazy val envelopeSentLogger      = LoggerFactory.getLogger("fssi.scp.ast.envelope.sent")

  private lazy val envelopeSavedLogger = LoggerFactory.getLogger("fssi.scp.ast.envelope.local")

  override def debug(message: String, cause: Option[Throwable]): Stack[Unit] = Stack {
    if (cause.isDefined) logger.debug(message, cause.get)
    else logger.debug(message)
  }

  override def info(message: String, cause: Option[Throwable]): Stack[Unit] = Stack {
    if (cause.isDefined) logger.info(message, cause.get)
    else logger.info(message)
  }

  override def warn(message: String, cause: Option[Throwable]): Stack[Unit] = Stack {
    if (cause.isDefined) logger.warn(message, cause.get)
    else logger.warn(message)
  }

  override def error(message: String, cause: Option[Throwable]): Stack[Unit] = Stack {
    if (cause.isDefined) logger.error(message, cause.get)
    else logger.error(message)
  }

  override def infoReceivedEnvelope[M <: Message](envelope: Envelope[M]): Stack[Unit] = Stack {
    infoEnvelope(envelopeReceivedLogger, "RECV", envelope)
    infoSavedEnvelope(envelope)
  }

  override def infoSentEnvelope[M <: Message](envelope: Envelope[M]): Stack[Unit] = Stack {
    infoEnvelope(envelopeSentLogger, "SEND", envelope)
    infoSavedEnvelope(envelope)
  }

  private def infoEnvelope[M <: Message](log: Logger, prefix: String, envelope: Envelope[M]): Unit = {
    if (logger.isInfoEnabled) {
      val nodeId = envelope.statement.from
      val idx = envelope.statement.slotIndex

      envelope.statement.message match {
        case x: Message.Nomination =>
          logger.info(s"[$prefix]nom$idx: $nodeId -> ${} voted ${x.voted.size}, accepted ${x.accepted.size}")
        case x: Message.Prepare =>
          logger.info(s"[$prefix]prepare$idx: $nodeId -> b.c=${x.b.counter}, p'.c=${x.`p'`
            .map(_.counter)}, p.c=${x.p.map(_.counter)}, c.n=${x.`c.n`}, h.n=${x.`h.n`}")
        case x: Message.Confirm =>
          logger.info(
            s"[$prefix]confirm$idx: $nodeId -> b.c=${x.b.counter}, p.n=${x.`p.n`}, c.n=${x.`c.n`}, h.n=${x.`h.n`}")
        case x: Message.Externalize =>
          logger.info(s"[$prefix]externalize$idx: $nodeId -> c.n=${x.`c.n`}, h.n=${x.`h.n`}")
      }
    }
  }

  private def infoSavedEnvelope[M <: Message](envelope: Envelope[M]): Unit = {
    if(envelopeSavedLogger.isDebugEnabled) {
      val idx = envelope.statement.slotIndex
      val slotIndex = envelope.statement.slotIndex
      val bs        = BallotStatus.getInstance(slotIndex)
      val ns        = NominationStatus.getInstance(slotIndex)
      envelopeSavedLogger.debug("")
      envelopeSavedLogger.debug("==== LOCAL NOMINATION ==============================================================")
      ns.latestNominations.foreach { map =>
        map.foreach {
          case (nodeId, env) =>
            val node = nodeId.asBytesValue.bcBase58
            val nom  = env.statement.message
            envelopeSavedLogger.debug(
              s"=   $idx, $node -> voted ${nom.voted.size}, accepted ${nom.accepted.size}")
        }
      }
      envelopeSavedLogger.debug("====================================================================================")

      envelopeSavedLogger.debug("")
      envelopeSavedLogger.debug("==== LOCAL BALLOTMESSAGE ===========================================================")
      bs.latestEnvelopes.foreach { map =>
        map.foreach {
          case (nodeId, env) =>
            val node = nodeId.asBytesValue.bcBase58
            env.statement.message match {
              case x: Message.Nomination =>
                envelopeSavedLogger.debug(
                  s"=    $idx, $nodeId -> nom: voted ${x.voted.size}, accepted ${x.accepted.size}")
              case x: Message.Prepare =>
                envelopeSavedLogger.debug(
                  s"=    $idx, $nodeId -> prepare: b.c=${x.b.counter}, p'.c=${x.`p'`
                    .map(_.counter)}, p.c=${x.p.map(_.counter)}, c.n=${x.`c.n`}, h.n=${x.`h.n`}")
              case x: Message.Confirm =>
                envelopeSavedLogger.debug(
                  s"=    $idx, $nodeId -> confirm: b.c=${x.b.counter}, p.n=${x.`p.n`}, c.n=${x.`c.n`}, h.n=${x.`h.n`}")
              case x: Message.Externalize =>
                envelopeSavedLogger.debug(
                  s"=    $idx, $nodeId -> externalize: c.n=${x.`c.n`}, h.n=${x.`h.n`}")
            }
        }
      }
      envelopeSavedLogger.debug("====================================================================================")
      envelopeSavedLogger.debug("")
    }
  }
}

object LogServiceHandler {
  private val instance = new LogServiceHandler
  trait Implicits {
    implicit val logServiceHandler: LogServiceHandler = instance
  }
  object implicits extends Implicits
}
