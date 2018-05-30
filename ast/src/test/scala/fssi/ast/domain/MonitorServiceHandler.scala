package fssi.ast.domain

import cats.Id

trait MonitorServiceHandler extends MonitorService.Handler[Id]{
  override def startNow(): Id[Long] = super.startNow()

  override def timePassed(start: Long): Id[Long] = super.timePassed(start)
}

object MonitorServiceHandler {
  trait Implicits {
    implicit val monitorServiceHandler = new MonitorServiceHandler {}
  }
  object implicits extends Implicits
}
