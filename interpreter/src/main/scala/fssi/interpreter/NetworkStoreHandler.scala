package fssi.interpreter

import java.nio.charset.Charset

import bigknife.sop._
import fssi.ast.domain.{NetworkStore, Node}
import io.circe.syntax._
import io.circe.parser._
import jsonCodec._
import better.files._

class NetworkStoreHandler extends NetworkStore.Handler[Stack] {
  override def saveNode(node: Node): Stack[Node] = Stack { setting =>
    val nodeJson = node.asJson.noSpaces
    //save to .node file
    File(setting.nodeJsonFile).overwrite(nodeJson)
    node
  }

  override def currentNode(): Stack[Option[Node]] = Stack { setting =>
    val f = File(setting.nodeJsonFile)
    if (f.exists) {
      val nodeJson = File(setting.nodeJsonFile).contentAsString(Charset.forName("utf-8"))
      parse(nodeJson).toOption.map(_.as[Node]).flatMap(_.toOption).map { node =>
        node.copy(
          accountPrivateKey =
            if (setting.boundAccount.isDefined) setting.boundAccount.get.privateKeyData
            else node.accountPrivateKey)
      }
    } else None

  }
}

object NetworkStoreHandler {
  private val instance = new NetworkStoreHandler
  trait Implicits {
    implicit val networkStoreHandler: NetworkStoreHandler = instance
  }
}
