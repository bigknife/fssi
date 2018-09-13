package fssi
package corenode

import interpreter._, Setting._
import interpreter.scp._
import handler._

/** FSSI CoreNode Main
  */
object CoreNodeMain extends App {

  val defaultCoreNodeSetting: CoreNodeSetting = CoreNodeSetting(
    workingDir = new java.io.File(new java.io.File(System.getProperty("user.home")), ".fssi"),
    password = Array.emptyByteArray,
    consensusConnect = ConsensusConnect.DuckConnect
  )
  CoreNodeSettingParser.parse(args, defaultCoreNodeSetting) match {
    case Some(setting) =>
      lazy val newSetting: Setting.CoreNodeSetting = setting.copy(consensusConnect = ConsensusConnect(getSetting = () => newSetting))
      startup(newSetting)
    case _ =>
  }
}
