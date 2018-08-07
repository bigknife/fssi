package fssi
package corenode

import interpreter.Setting._
import handler._

/** FSSI CoreNode Main
  */
object CoreNodeMain extends App {
  val defaultCoreNodeSetting = CoreNodeSetting(
    workingDir = new java.io.File(new java.io.File(System.getProperty("user.home")), ".fssi"),
    password = Array.emptyByteArray
  )
  CoreNodeSettingParser.parse(args, defaultCoreNodeSetting) match {
    case Some(setting) => startup(setting)
    case _ =>
  }
}
