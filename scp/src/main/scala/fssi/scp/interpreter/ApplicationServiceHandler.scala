package fssi.scp
package interpreter

import bigknife.sop._

import fssi.scp.ast._
import fssi.scp.types._

class ApplicationServiceHandler extends ApplicationService.Handler[Stack] {}

object ApplicationServiceHandler {
  val instance = new ApplicationServiceHandler

  trait Implicits {
    implicit val scpApplicationServiceHandler: ApplicationServiceHandler = instance
  }
}
