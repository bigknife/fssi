package fssi.interpreter

import fssi.ast.domain.LogService

class LogServiceHandler extends LogService.Handler[Stack] {}

object LogServiceHandler {
  trait Implicits {
    implicit val logServiceHandler: LogServiceHandler = new LogServiceHandler
  }
  object implicits extends Implicits
}
