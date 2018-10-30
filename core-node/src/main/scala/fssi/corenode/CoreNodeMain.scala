package fssi
package corenode

import fssi.ast.uc.CoreNodeProgram
import fssi.ast._
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.interpreter.scp.SCPEnvelope
import fssi.interpreter.{Setting, StackConsoleMain}
import fssi.scp.types.{Envelope, Message}
import fssi.types.biz.{JsonMessage, JsonMessageHandler, Transaction}
import fssi.scp.interpreter.store.Var

/** FSSI CoreNode Main
  */
object CoreNodeMain extends StackConsoleMain[CoreNodeSetting] {
  private val instance               = CoreNodeProgram.instance
  private val _setting: Var[Setting] = Var.empty
  override def setting: Setting      = _setting.unsafe()

  val defaultCoreNodeSetting: CoreNodeSetting = CoreNodeSetting(
    workingDir = new java.io.File(new java.io.File(System.getProperty("user.home")), ".fssi"),
    password = Array.emptyByteArray
  )

  override def cmdArgs(xs: Array[String]): Option[CoreNodeSetting] = {
    CoreNodeSettingParser.parse(args, defaultCoreNodeSetting)
  }

  override def setting(c: CoreNodeSetting): Setting = {
    _setting := c
    c
  }

  override def program(cmdArgs: Option[CoreNodeSetting],
                       setting: Setting): StackConsoleMain.Effect = cmdArgs match {
    case Some(coreNodeSetting) =>
      import instance._
      for {
        node <- if (coreNodeSetting.isFullFunctioning) startupFull(coreNodeSetting.workingDir, consensusMessageHandler(), applicationMessageHandler()) else startupSemi(coreNodeSetting.workingDir, consensusMessageHandler())
        _ <- Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
          override def run(): Unit = {
            shutdow
            ()
          }
        }))
        _ <- Thread.currentThread().join()
      } yield ()
    case _ =>
  }

  /*
  override def handleTransaction(transaction: Transaction): Effect = {
    for {
      receipt <- instance.handleTransaction(transaction)
      _ <- log.info(s"transaction handled: ${receipt.success}")
    } yield ()
  }

  override def handleScpEnvelope(envelope: Envelope[Message]): Effect = {
    for {
      _ <- instance.processMessage(SCPEnvelope(envelope))
    } yield ()
  }
 */

}
