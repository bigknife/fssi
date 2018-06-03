package fssi.interpreter

import fssi.ast.domain.Node
import org.scalatest._

class NetworkStoreHandlerSpec extends FunSuite {
  val setting: Setting = Setting()
  val networkStoreHandler = new NetworkStoreHandler

  test("network store") {
    val node: Node = Node(
      18085,
      "127.0.0.1",
      Node.Type.Nymph,
      None,
      Vector.empty
    )

    networkStoreHandler.saveNode(node)(setting).unsafeRunSync()

    val loadedNode = networkStoreHandler.currentNode()(setting).unsafeRunSync()

    assert(loadedNode.isDefined)
    assert(loadedNode.contains(node))
    assert(node.id == loadedNode.get.id)
  }
}
