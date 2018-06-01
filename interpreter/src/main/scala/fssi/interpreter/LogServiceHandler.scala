package fssi.interpreter

import fssi.ast.domain.LogService
import org.slf4j.LoggerFactory

/** log service, use logback as the backend
  *
  */
class LogServiceHandler extends LogService.Handler[Stack] {
  // all ast logs use the same name: fssi.ast
  private val logger = LoggerFactory.getLogger("fssi.ast")

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
}

object LogServiceHandler {
  trait Implicits {
    implicit val logServiceHandler: LogServiceHandler = new LogServiceHandler
  }
  object implicits extends Implicits
}
