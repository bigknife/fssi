package fssi
package scp
package interpreter

import fssi.scp.ast._
import fssi.scp.interpreter.store.{BallotStatus, NominationStatus}
import fssi.scp.types.{Envelope, Message}
import org.slf4j.LoggerFactory
import fssi.scp.types.implicits._
import fssi.base.BytesValue.implicits._

/** log service, use logback as the backend
  *
  */
class LogServiceHandler extends LogService.Handler[Stack] {
  // all ast logs use the same name: fssi.ast
  private val logger = LoggerFactory.getLogger("fssi.scp.ast")

  private lazy val envelopeLogger      = LoggerFactory.getLogger("fssi.scp.ast.envelope")
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

  override def infoEnvelope[M <: Message](envelope: Envelope[M]): Stack[Unit] = Stack {
    if (envelopeLogger.isInfoEnabled) {
      envelope.statement.message match {
        case x: Message.Nomination =>
          envelopeLogger.info(s"nom: voted ${x.voted.size}, accepted ${x.accepted.size}")
        case x: Message.Prepare =>
          envelopeLogger.info(s"prepare: b.c=${x.b.counter}, p'.c=${x.`p'`
            .map(_.counter)}, p.c=${x.p.map(_.counter)}, c.n=${x.`c.n`}, h.n=${x.`h.n`}")
        case x: Message.Confirm =>
          envelopeLogger.info(
            s"confirm: b.c=${x.b.counter}, p.n=${x.`p.n`}, c.n=${x.`c.n`}, h.n=${x.`h.n`}")
        case x: Message.Externalize =>
          envelopeLogger.info(s"externalize: c.n=${x.`c.n`}, h.n=${x.`h.n`}")

      }
      val slotIndex = envelope.statement.slotIndex
      val bs        = BallotStatus.getInstance(slotIndex)
      val ns        = NominationStatus.getInstance(slotIndex)
      envelopeSavedLogger.info("==== LOCAL NOMINATION ====")
      ns.latestNominations.foreach { map =>
        map.foreach {
          case (nodeId, env) =>
            val node = nodeId.asBytesValue.bcBase58
            val nom  = env.statement.message
            envelopeSavedLogger.info(
              s"=   $node -> voted ${nom.voted.size}, accepted ${nom.accepted.size}")
        }
      }
      envelopeSavedLogger.info("==========================")

      envelopeSavedLogger.info("==== LOCAL BALLOTMESSAGE ====")
      bs.latestEnvelopes.foreach { map =>
        map.foreach {
          case (nodeId, env) =>
            val node = nodeId.asBytesValue.bcBase58
            env.statement.message match {
              case x: Message.Nomination =>
                envelopeSavedLogger.info(
                  s"=    $nodeId -> nom: voted ${x.voted.size}, accepted ${x.accepted.size}")
              case x: Message.Prepare =>
                envelopeSavedLogger.info(
                  s"=    $nodeId -> prepare: b.c=${x.b.counter}, p'.c=${x.`p'`
                    .map(_.counter)}, p.c=${x.p.map(_.counter)}, c.n=${x.`c.n`}, h.n=${x.`h.n`}")
              case x: Message.Confirm =>
                envelopeSavedLogger.info(
                  s"=    $nodeId -> confirm: b.c=${x.b.counter}, p.n=${x.`p.n`}, c.n=${x.`c.n`}, h.n=${x.`h.n`}")
              case x: Message.Externalize =>
                envelopeSavedLogger.info(
                  s"=    $nodeId -> externalize: c.n=${x.`c.n`}, h.n=${x.`h.n`}")
            }
        }
      }
      envelopeSavedLogger.info("==========================")
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
