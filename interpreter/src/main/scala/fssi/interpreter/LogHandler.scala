package fssi
package interpreter

import types._, exception._
import ast._
import org.slf4j.LoggerFactory

/** log service, use logback as the backend
  *
  */
class LogHandler extends Log.Handler[Stack] {
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

object LogHandler {
  private val instance = new LogHandler
  trait Implicits {
    implicit val logServiceHandler: LogHandler = instance
  }
  object implicits extends Implicits
}
