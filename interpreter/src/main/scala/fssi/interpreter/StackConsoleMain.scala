package fssi.interpreter

import bigknife.sop._
import bigknife.sop.implicits._
import fssi.ast._
import fssi.ast.uc.ToolProgram
import fssi.types.base.RandomSeed
import org.slf4j.LoggerFactory

trait StackConsoleMain[C] {

  import StackConsoleMain._

  lazy val log = LoggerFactory.getLogger(getClass)

  def cmdArgs(xs: Array[String]): Option[C]
  def setting(c: C): Setting = Setting.DefaultSetting
  def program(cmdArgs: Option[C], setting: Setting): Effect

  def main(args: Array[String]): Unit = {
    // run program
    lazy val _c: Option[C] = cmdArgs(args)
    lazy val _setting      = _c.map(setting).getOrElse(Setting.DefaultSetting)
    lazy val _p            = program(_c, _setting)
    runner.runIOAttempt(_p, _setting).unsafeRunSync() match {
      case Left(t)  => log.error("Main Program Error", t)
      case Right(_) => log.info("Main Program Exit")
    }
  }

  implicit def lift[A](a: A): Program[A] = a.pureSP[blockchain.Model.Op]

}

object StackConsoleMain {
  type Program[A] = SP[blockchain.Model.Op, A]
  type Effect     = Program[Unit]
}
