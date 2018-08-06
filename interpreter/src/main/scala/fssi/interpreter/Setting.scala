package fssi
package interpreter

import types._
import java.io._

sealed trait Setting

object Setting {
  case object DefaultSetting extends Setting
  case class CoreNodeSetting(workingDir: File, password: String) extends Setting

  def defaultInstance: Setting = DefaultSetting

}
