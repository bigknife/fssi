package fssi.interpreter

import fssi.ast.domain.Node
import fssi.ast.domain.types.BytesValue
import org.scalatest._

class NetworkStoreHandlerSpec extends FunSuite {
  val setting: Setting    = Setting()
  val networkStoreHandler = new NetworkStoreHandler

  test("network store") {
    val node: Node = Node(
      Node.Address("127.0.0.1", 18085),
      Node.Type.Nymph,
      BytesValue.decodeHex("0294282aff25dfc77cf7ac3725078a5eb6098f1eeac44280c4eb2931f97325b0d6"),
      BytesValue.Empty,
      Vector.empty
    )

    networkStoreHandler.saveNode(node)(setting).unsafeRunSync()

    val loadedNode = networkStoreHandler.currentNode()(setting).unsafeRunSync()

    assert(loadedNode.isDefined)
    assert(loadedNode.contains(node))
    assert(node.id == loadedNode.get.id)
  }
}
