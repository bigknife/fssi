package fssi.utils
package trie

import Node.{Branch, Empty, Extension, Leaf}

trait Trie {
  def hash: Option[Hash]

  // store the nodes, key is the hash
  def store: Store

  def isEmpty: Boolean = hash.isEmpty

}

object Trie {

  protected[Trie] case class SimpleTrie(hash: Option[Hash], store: Store) extends Trie {

    override def toString: String = {
      if (isEmpty) s"Trie<EMPTY>"
      else s"Trie<hash(${BytesValue(hash.get).hex})>"
    }

    override def equals(obj: scala.Any): Boolean = (obj, this) match {
      case (SimpleTrie(Some(x), _), SimpleTrie(Some(y), _)) => Hash.equal(x, y)
      case (SimpleTrie(None, _), SimpleTrie(None, _))       => true
      case _                                                => false
    }
  }

  def empty(): Trie = SimpleTrie(None, Store.memory())
  def empty(store: Store): Trie = SimpleTrie(None, store)

  object ops {

    implicit final class TireOps(trie: Trie)(implicit _serializer: Node.Serializer) {

      lazy val combinator: NodeCombinator = new NodeCombinator {
        lazy val serializer: Node.Serializer = _serializer
        lazy val store: Store                = trie.store
      }

      def put(key: Key, value: Value): Trie =
        if (trie.isEmpty) {
          val node = Node.leaf(key, value)
          val hash = node.hash
          trie.store.save(hash, _serializer.toBytes(node))
          withHash(hash)

        } else {
          trie.store
            .load(trie.hash.get)
            .flatMap(_serializer.fromBytes)
            .map(x => {
              val leaf = Node.leaf(Nibble.Sequence(key), value)
              putNodeToNode(x, leaf)
              //putToNode(x, Nibble.Sequence(key), value)
            }) match {
            case Some(Extension(seq, refKey)) if seq.unprefixedWithExtension.isEmpty =>
              withHash(refKey)
            case Some(node) =>
              val hash = node.hash
              withHash(hash)
            case _ => throw new IllegalStateException("this should not happen")
          }

        }
      def getNode(key: Key): Option[Node] = {
        val nibbles = Nibble.Sequence(key)
        for {
          hash      <- trie.hash
          nodeValue <- trie.store.load(hash)
          node      <- _serializer.fromBytes(nodeValue)
          x         <- getNodeFromNode(node, nibbles)
        } yield x
      }
      def getValue(key: Key): Option[Value] = getNode(key).flatMap {
        case Branch(_, valueOpt) => valueOpt
        case Leaf(_, value)      => Some(value)
        case _                   => None
      }

      def withStore(store: Store): Trie = SimpleTrie(trie.hash, store)
      def withHash(hash: Hash): Trie    = SimpleTrie(Some(hash), trie.store)

      private def getNodeFromNode(target: Node, seq: Nibble.Sequence): Option[Node] = target match {
        case Empty => None
        case x @ Leaf(encodedPath, value) =>
          val path = encodedPath.unprefixedWithLeaf
          if (seq == path) Some(x) else None
        case x @ Extension(encodedPath, key) =>
          val path      = encodedPath.unprefixedWithExtension
          val maxPrefix = findMaxPrefix(path, seq)
          val referNode = trie.store.load(key).flatMap(_serializer.fromBytes).get
          getNodeFromNode(referNode, seq.drop(maxPrefix.size))
        /*
          val pathRemain = path.drop(maxPrefix.size)
          if (pathRemain != 0) None
          else {
            val nextNode   = serializer.fromBytes(trie.store.load(x.hash).get).get
            val currRemain = seq.drop(maxPrefix.size)
            getNodeFromNode(nextNode, currRemain)
          }
         */

        case x @ Branch(children, valueOpt) =>
          if (seq.isEmpty) Some(x)
          else {
            val nextOpt = children(seq.head.value.toInt)
            if (nextOpt.isEmpty) None
            else {
              val nextNode = nextOpt.flatMap(trie.store.load).flatMap(_serializer.fromBytes)
              //todo must not be empty
              if (nextNode.isDefined) {
                getNodeFromNode(nextNode.get, seq.drop(1))
              } else None
              //serializer.fromBytes(trie.store.load(nextOpt.get).get).get

            }
          }
      }

      private def putNodeToNode(target: Node, current: Node): Node = (target, current) match {
        case (Empty, _) => Empty
        case (x, Empty) => x
        case (x @ Leaf(_, _), y @ Leaf(_, _)) =>
          combinator.combineLeafWithLeaf(x, y)

        case (x @ Leaf(_, _), y @ Extension(_, _)) =>
          combinator.combineLeafWithExtension(x, y)

        case (Leaf(_, _), Branch(_, _)) =>
          throw new IllegalStateException("can't insert branch into leaf")

        case (x @ Extension(_, _), y @ Leaf(_, _)) =>
          combinator.combineExtensionWithLeaf(x, y)

        case (x @ Extension(_, _), y @ Extension(_, _)) =>
          combinator.combineExtensionWithExtension(x, y)

        case (Extension(_, _), Branch(_, _)) =>
          throw new IllegalStateException("can't insert branch into extension")

        case (x @ Branch(_, _), y @ Extension(_, _)) =>
          combinator.combineBranchWithExtension(x, y)

        case (x @ Branch(_, _), y @ Leaf(_, _)) =>
          combinator.combineBranchWithLeaf(x, y)

        case (Branch(_, _), Branch(_, _)) =>
          throw new IllegalStateException("can't insert branch into branch")

      }

      private def findMaxPrefix(seq1: Nibble.Sequence, seq2: Nibble.Sequence): Nibble.Sequence = {
        def find0(s1: Nibble.Sequence,
                  s2: Nibble.Sequence,
                  acc: Nibble.Sequence): Nibble.Sequence = {
          if (s1.isEmpty || s2.isEmpty || s1.head != s2.head) acc
          else find0(s1.drop(1), s2.drop(1), acc :+ s1.head)
        }
        find0(seq1, seq2, Nibble.Sequence.empty)
      }
    }
  }
}
