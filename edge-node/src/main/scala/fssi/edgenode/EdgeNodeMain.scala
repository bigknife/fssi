package fssi
package edgenode
import java.io.File

import fssi.ast.uc.EdgeNodeProgram
import fssi.interpreter.Setting.EdgeNodeSetting
import fssi.interpreter.StackConsoleMain.Effect
import fssi.interpreter.{Setting, StackConsoleMain}
import fssi.types.ServiceResource
import bigknife.jsonrpc._

object EdgeNodeMain extends StackConsoleMain[EdgeNodeSetting] {
  private val instance = EdgeNodeProgram.instance

  val defaultEdgeNodeSetting = EdgeNodeSetting(
    workingDir = new File(System.getProperty("user.home"), ".fssi"),
    password = Array.emptyByteArray
  )

  def resource: EdgeNodeSetting => ServiceResource =
    edgeNodeSetting =>
      server.run(name = "edge",
                 version = "v1",
                 resource = EdgeJsonRpcResource,
                 port = edgeNodeSetting.config.jsonRPCConfig.port,
                 host = edgeNodeSetting.config.jsonRPCConfig.host)

  override def cmdArgs(xs: Array[String]): Option[EdgeNodeSetting] =
    EdgeNodeSettingParser.parse(xs, defaultEdgeNodeSetting)

  override def setting(c: EdgeNodeSetting): Setting = c

  override def program(cmdArgs: Option[EdgeNodeSetting], setting: Setting): Effect = {
    import instance._
    cmdArgs match {
      case Some(edgeNodeSetting) =>
        for {
          node <- startup(applicationMessageHandler(),
                          clientMessageHandler(),
                          resource(edgeNodeSetting))
          _ <- Runtime.getRuntime.addShutdownHook(new Thread() {
            override def run(): Unit = { shutdown(node._1, node._2); () }
          })
        } yield ()
      case _ =>
    }
  }

}
