package fssi
package edgenode

import interpreter.Setting._
import handler._

object EdgeNodeMain extends App {
  val defaultEdgeNodeSetting = EdgeNodeSetting(
    workingDir = new java.io.File(new java.io.File(System.getProperty("user.home")), ".fssi"),
    password = Array.emptyByteArray
  )

  EdgeNodeSettingParser.parse(args, defaultEdgeNodeSetting) match {
    case Some(setting) =>  startup(setting)
    case _ =>
  }
}
