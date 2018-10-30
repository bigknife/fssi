package fssi
package corenode

import fssi.ast.uc.CoreNodeProgram
import fssi.ast._
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.interpreter.scp.SCPEnvelope
import fssi.interpreter.{Setting, StackConsoleMain}
import fssi.scp.types.{Envelope, Message}
import fssi.types.biz.{JsonMessage, JsonMessageHandler, Node, Transaction}
import fssi.scp.interpreter.store.Var

/** FSSI CoreNode Main
  */
object CoreNodeMain extends StackConsoleMain[CoreNodeSetting] {
  private val instance = CoreNodeProgram.instance

  val defaultCoreNodeSetting: CoreNodeSetting = CoreNodeSetting(
    workingDir = new java.io.File(new java.io.File(System.getProperty("user.home")), ".fssi"),
    password = Array.emptyByteArray
  )

  override def cmdArgs(xs: Array[String]): Option[CoreNodeSetting] = {
    CoreNodeSettingParser.parse(xs, defaultCoreNodeSetting)
  }

  override def setting(c: CoreNodeSetting): Setting = {
    c
  }

  override def program(cmdArgs: Option[CoreNodeSetting],
                       setting: Setting): StackConsoleMain.Effect = cmdArgs match {
    case Some(coreNodeSetting) =>
      import instance._
      for {
        node <- if (coreNodeSetting.isFullFunctioning)
          startupFull(coreNodeSetting.workingDir,
                      consensusMessageHandler(),
                      applicationMessageHandler()).map(x => (x._1, Option(x._2)))
        else
          startupSemi(coreNodeSetting.workingDir, consensusMessageHandler())
            .map((_, Option.empty[Node.ApplicationNode]))
        _ <- Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
          override def run(): Unit = {
            shutdown(node._1, node._2)
            ()
          }
        }))
        _ <- Thread.currentThread().join()
      } yield ()
    case _ =>
  }
}
