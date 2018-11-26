package fssi
package scp
package interpreter

import fssi.scp.ast._
import fssi.scp.types.{Envelope, Message}
import org.slf4j.LoggerFactory

/** log service, use logback as the backend
  *
  */
class LogServiceHandler extends LogService.Handler[Stack] {
  // all ast logs use the same name: fssi.ast
  private val logger = LoggerFactory.getLogger("fssi.scp.ast")

  private lazy val envelopeLogger = LoggerFactory.getLogger("fssi.scp.ast.envelope")

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
    if(envelopeLogger.isInfoEnabled) {
      envelope.statement.message match {
        case x: Message.Nomination =>
          envelopeLogger.info(s"nom: voted ${x.voted.size}, accepted ${x.accepted.size}")
        case x: Message.Prepare =>
          envelopeLogger.info(s"prepare: b.c=${x.b.counter}, p'.c=${x.`p'`.map(_.counter)}, p.c=${x.p.map(_.counter)}, c.n=${x.`c.n`}, h.n=${x.`h.n`}")
        case x: Message.Confirm =>
          envelopeLogger.info(s"confirm: b.c=${x.b.counter}, p.n=${x.`p.n`}, c.n=${x.`c.n`}, h.n=${x.`h.n`}")
        case x: Message.Externalize =>
          envelopeLogger.info(s"externalize: c.n=${x.`c.n`}, h.n=${x.`h.n`}")
      }

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
