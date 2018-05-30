package fssi.ast.domain

import cats.Id

trait LogServiceHandler extends LogService.Handler[Id]{
  override def debug(message: String, cause: Option[Throwable]): Id[Unit] = super.debug(message, cause)

  override def info(message: String, cause: Option[Throwable]): Id[Unit] = super.info(message, cause)

  override def warn(message: String, cause: Option[Throwable]): Id[Unit] = super.warn(message, cause)

  override def error(message: String, cause: Option[Throwable]): Id[Unit] = super.error(message, cause)
}

object LogServiceHandler {
  trait Implicits {
    implicit  val logServiceHandler = new LogServiceHandler {}
  }
  object implicits extends Implicits
}