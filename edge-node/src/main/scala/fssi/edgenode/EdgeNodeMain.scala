package fssi
package edgenode
import java.io.File

import fssi.ast.uc.EdgeNodeProgram
import fssi.interpreter.Setting.EdgeNodeSetting
import fssi.interpreter.StackConsoleMain.Effect
import fssi.interpreter.{Setting, StackConsoleMain}

object EdgeNodeMain extends StackConsoleMain[EdgeNodeSetting] {
  private val instance = EdgeNodeProgram.instance

  val defaultEdgeNodeSetting = EdgeNodeSetting(
    workingDir = new File(System.getProperty("user.home"), ".fssi"),
    password = Array.emptyByteArray
  )

  override def cmdArgs(xs: Array[String]): Option[EdgeNodeSetting] =
    EdgeNodeSettingParser.parse(xs, defaultEdgeNodeSetting)

  override def setting(c: EdgeNodeSetting): Setting = c

  override def program(cmdArgs: Option[EdgeNodeSetting], setting: Setting): Effect = {
    import instance._
    cmdArgs match {
      case Some(edgeNodeSetting) =>
        for {
          node <- startup(applicationMessageHandler(edgeNodeSetting),
                          clientMessageHandler(edgeNodeSetting))
          _ <- Runtime.getRuntime.addShutdownHook(new Thread() {
            override def run(): Unit = { shutdown(node._1, node._2); () }
          })
          _ <- Thread.currentThread().join()
        } yield ()
      case _ =>
    }
  }

}
