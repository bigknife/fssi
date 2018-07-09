package fssi.interpreter

import fssi.ast.domain._

class MonitorServiceHandler extends MonitorService.Handler[Stack] {

  override def startNow(): Stack[Long] = Stack {
    System.currentTimeMillis()
  }

  override def timePassed(start: Long): Stack[Long] = Stack {
    System.currentTimeMillis() - start
  }
}

object MonitorServiceHandler {
  private val instance = new MonitorServiceHandler
  trait Implicits {
    implicit val monitorServiceHandler: MonitorServiceHandler = instance
  }
  object implicits extends Implicits
}
