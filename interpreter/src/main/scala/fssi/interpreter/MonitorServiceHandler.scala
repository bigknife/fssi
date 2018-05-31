package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class MonitorServiceHandler extends MonitorService.Handler[Stack]{

}

object MonitorServiceHandler {
  trait Implicits {
    implicit val monitorServiceHandler: MonitorServiceHandler = new MonitorServiceHandler
  }
  object implicits extends Implicits
}
