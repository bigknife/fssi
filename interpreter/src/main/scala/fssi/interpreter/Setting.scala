package fssi
package interpreter

import types._
import java.io._
import bigknife.scalap.world.Connect

sealed trait Setting

object Setting {

  /** default setting
    */
  case object DefaultSetting extends Setting

  /** Setting for command line tool
    */
  case class ToolSetting() extends Setting

  /** P2P node setting
    */
  sealed trait P2PNodeSetting extends Setting {
    def workingDir: File
    def password: Array[Byte]
    def configReader: ConfigReader
  }

  /** setting for running core node
    */
  case class CoreNodeSetting(workingDir: File, password: Array[Byte], consensusConnect: Connect)
      extends P2PNodeSetting {
    private lazy val configFile: File = new File(workingDir, "fssi.conf")
    lazy val configReader: ConfigReader = ConfigReader(configFile)
  }

  case class EdgeNodeSetting(workingDir: File, password: Array[Byte]) extends P2PNodeSetting {
    private lazy val configFile: File = new File(workingDir, "fssi.conf")
    lazy val configReader: ConfigReader = ConfigReader(configFile)
  }

  def defaultInstance: Setting = DefaultSetting

}
