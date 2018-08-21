package fssi
package corenode

import interpreter.Setting._
import interpreter.scp._
import handler._

/** FSSI CoreNode Main
  */
object CoreNodeMain extends App {

  object consensusConnect extends ConsensusConnect

  val defaultCoreNodeSetting = CoreNodeSetting(
    workingDir = new java.io.File(new java.io.File(System.getProperty("user.home")), ".fssi"),
    password = Array.emptyByteArray,
    consensusConnect = consensusConnect
  )
  CoreNodeSettingParser.parse(args, defaultCoreNodeSetting) match {
    case Some(setting) => startup(setting)
    case _ =>
  }
}
