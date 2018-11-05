package fssi.store.mpt

import fssi.store.core.KVStore
import org.bouncycastle.jcajce.provider.digest.SHA3

/** MPT: Merkle Patricial Tree
  * treeName -> root  hash
  * root  hash -> root node
  */
trait MPT {
  def store: KVStore

  def name: String
  def nodesFromData(data: Data): Option[Node]

  def rootHash: Option[Hash] = {
    store.get(name.getBytes("utf-8")).map(Hash)
  }
  def rootNode: Option[Node] = {
    for {
      key       <- rootHash
      dataBytes <- store.get(key.value)
      node      <- nodesFromData(Data(dataBytes))
    } yield node
  }

  def put(key: Key, data: Data): Unit = {
    //todo: transaction support.

    // build leaf node with key and data
    val keyHash = hashKey(key)
    val leafNode = Node.Leaf(Path.LeafPath(keyHash.value), data)

    // insert the leaf node to root node(combining), then get a new root node
    val newRootNode = combineLeaf(leafNode, rootNode.getOrElse(Node.Null))
    val newRootHash = hashNode(newRootNode)

    // update tree name to refer to the new root node hash, and delete the old root node
    rootHash.foreach(x => store.delete(x.value))
    store.put(name.getBytes("utf-8"), newRootHash.value)
  }

  private def hashKey(key: Key): Hash = Hash(new SHA3.Digest256().digest(key.value))
  private def hashNode(node: Node): Hash = ???

  // core method: combine a leafNode and a node
  private def combineLeaf(leaf: Node.Leaf, targetNode: Node): Node = {
    targetNode match {
      case Node.Null =>
    }

    ???
  }
}
