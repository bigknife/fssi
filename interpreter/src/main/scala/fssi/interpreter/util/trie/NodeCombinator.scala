package fssi.interpreter.util.trie

import org.slf4j.LoggerFactory
import Node._

/** combine two node, return combined node
  *
  */
trait NodeCombinator {

  val store: Store
  val serializer: Node.Serializer

  private val logger = LoggerFactory.getLogger("fssi.interpreter.trie")

  private def hashAndSave(n: Node): Node = {
    store.save(n.hash, serializer.toBytes(n))
    n
  }

  def load(key: StoreKey): Option[StoreValue] = store.load(key)

  def fromBytes(bytes: Array[Byte]): Option[Node] = serializer.fromBytes(bytes)

  def combineLeafWithLeaf(target: Leaf, current: Leaf): Node = {
    logger.debug(s"$target <- $current")

    val targetPath  = target.encodedPath.unprefixedWithLeaf
    val currentPath = current.encodedPath.unprefixedWithLeaf
    if (targetPath == currentPath) {
      // 完全相同，替换掉原来的值
      hashAndSave(current)
    }
    else if(targetPath.isEmpty || currentPath.isEmpty) {
      // create a branch, set value to the value of the leaf of empty path
      val br = Node.emptyBranch().asInstanceOf[Branch]
      val br1 = br.updateValue(() => if (targetPath.isEmpty) Some(target.value) else Some(current.value))
      val slotIdx = if(targetPath.isEmpty) currentPath.head.value.toInt else targetPath.head.value.toInt
      val newSlotContent = if (targetPath.isEmpty) Node.leaf(currentPath.drop(1), current.value) else Node.leaf(targetPath.drop(1), target.value)
      hashAndSave(newSlotContent)
      val br2 = br1.updateChildAtIndex(slotIdx, Some(newSlotContent.hash))
      hashAndSave(br2)
    }
    else {
      val maxPrefix = findMaxPrefix(targetPath, currentPath)
      val targetRemain  = targetPath.drop(maxPrefix.size)
      val currentRemain = currentPath.drop(maxPrefix.size)

      val br = Node.emptyBranch().asInstanceOf[Branch]
      val brWithValue = br.updateValue { () =>
        if (targetRemain.isEmpty) Some(target.value)
        else if (currentRemain.isEmpty) Some(current.value)
        else None
      }

      val branch = {
        val v1 = if (!targetRemain.isEmpty) {
          val targetIdx = targetRemain.head.value.toInt

          val targetTail = Node.leaf(targetPath.drop(maxPrefix.size + 1), target.value)
          hashAndSave(targetTail)

          brWithValue.updateChildAtIndex(targetIdx, Some(targetTail.hash))
        } else brWithValue

        if (!currentRemain.isEmpty) {
          val currentIdx  = currentRemain.head.value.toInt
          val currentTail = Node.leaf(currentPath.drop(maxPrefix.size + 1), current.value)

          hashAndSave(currentTail)
          v1.updateChildAtIndex(currentIdx, Some(currentTail.hash))
        } else v1
      }

      hashAndSave(branch)
      val extension = Node.extension(maxPrefix, branch.hash)
      hashAndSave(extension)

      extension
    }

  }

  def combineLeafWithExtension(target: Leaf, current: Extension): Node = {
    logger.debug(s"$target <- $current")

    val targetPath  = target.encodedPath.unprefixedWithLeaf
    val currentPath = current.encodedPath.unprefixedWithExtension
    require(!targetPath.isEmpty && !currentPath.isEmpty)

    if (targetPath == currentPath) {
      // 完全相同，替换掉原来的值
      hashAndSave(current)
    } else {
      val maxPrefix     = findMaxPrefix(targetPath, currentPath)
      val targetRemain  = targetPath.drop(maxPrefix.size)
      val currentRemain = currentPath.drop(maxPrefix.size)
      require(!currentRemain.isEmpty,
              "when combine leaf with extension, extension remained key should not empty")

      val br = Node.emptyBranch().asInstanceOf[Branch]
      val brWithValue = br.updateValue { () =>
        if (targetRemain.isEmpty) Some(target.value)
        else None
      }

      val branch = {
        val v1 = if (!targetRemain.isEmpty) {
          val targetIdx = targetRemain.head.value.toInt

          val targetTail = Node.leaf(targetPath.drop(maxPrefix.size + 1), target.value)

          hashAndSave(targetTail)
          brWithValue.updateChildAtIndex(targetIdx, Some(targetTail.hash))
        } else brWithValue

        val currentIdx   = currentRemain.head.value.toInt
        val extensionKey = currentPath.drop(maxPrefix.size + 1)
        if (extensionKey.isEmpty) {
          //直接把扩展节点的hashkey放到branch对应的位置
          v1.updateChildAtIndex(currentIdx, Some(current.key))
        } else {
          val currentTail = Node.extension(extensionKey, current.key)

          hashAndSave(currentTail)
          v1.updateChildAtIndex(currentIdx, Some(currentTail.hash))
        }
      }
      hashAndSave(branch)

      val extension = Node.extension(maxPrefix, branch.hash)
      hashAndSave(extension)
    }
  }

  def combineBranchWithLeaf(target: Branch, current: Leaf): Node = {
    logger.debug(s"$target <- $current")

    val currentPath = current.encodedPath.unprefixedWithLeaf
    // if current path is empty, update branch's value
    if (currentPath.isEmpty) {
      //todo may delete the target ?
      val newBranch = target.copy(value = Some(current.value))
      hashAndSave(newBranch)
    } else {
      val slotIdx = currentPath.head.value.toInt

      val newLeaf = Node.leaf(currentPath.drop(1), current.value).asInstanceOf[Leaf]
      hashAndSave(newLeaf)//need save?

      val referedOpt = target.children(slotIdx)

      // if the slot is empty, update to the new leaf(without head)
      if (referedOpt.isEmpty) {
        //todo may delete the current ?
        val newTarget = target.updateChildAtIndex(slotIdx, Some(newLeaf.hash))
        hashAndSave(newTarget)
      } else {
        val newNode = store.load(referedOpt.get).flatMap(serializer.fromBytes) match {
          case None =>
            throw new IllegalStateException(s"can't load node with hash(${referedOpt.get})")
          case Some(x @ Leaf(_, _))      => combineLeafWithLeaf(x, newLeaf)
          case Some(x @ Extension(_, _)) => combineExtensionWithLeaf(x, newLeaf)
          case Some(x @ Branch(_, _))    => combineBranchWithLeaf(x, newLeaf)
          case Some(Empty)               => newLeaf
        }
        //delete old content in slot
        //store.delete(referedOpt.get)
        //create new target
        // todo may delete old target
        val newTarget = target.updateChildAtIndex(slotIdx, Some(newNode.hash))
        hashAndSave(newTarget)
      }
    }
  }

  def combineExtensionWithLeaf(target: Extension, current: Leaf): Node = {
    logger.debug(s"$target <- $current")

    val targetPath  = target.encodedPath.unprefixedWithExtension
    val currentPath = current.encodedPath.unprefixedWithLeaf
    val maxPrefix         = findMaxPrefix(targetPath, currentPath)
    val targetPathRemain  = targetPath.drop(maxPrefix.size)
    val currentPathRemain = currentPath.drop(maxPrefix.size)

    if (targetPathRemain.isEmpty) {
      //todo may delete current
      val newLeaf = Node.leaf(currentPathRemain, current.value).asInstanceOf[Leaf]
      hashAndSave(newLeaf)
      store.load(target.key).flatMap(serializer.fromBytes) match {
        case None => throw new IllegalStateException(s"can't load node with hash(${target.key})")
        case Some(x @ Branch(_, _)) =>
          val newNode = combineBranchWithLeaf(x, newLeaf)
          //todo may delete x
          hashAndSave(newNode)
          //todo may delete old target
          val newTarget = target.copy(key = newNode.hash)
          hashAndSave(newTarget)
        case Some(x @ Extension(_, _)) =>
          throw new IllegalStateException("extension not allowed to refer to another extension")
        case Some(x @ Leaf(_, _)) =>
          val newNode = combineLeafWithLeaf(x, newLeaf)
          hashAndSave(newNode)
          val newTarget = target.copy(key = newNode.hash)
          hashAndSave(newTarget)
        case _ => throw new IllegalStateException("extension not allowed to refer to an empty")
      }
    } else {
      val br            = Node.emptyBranch().asInstanceOf[Branch]
      val targetSlotIdx = targetPathRemain.head.value.toInt
      val targetSlotted =
        Node.extension(targetPathRemain.drop(1), target.key).asInstanceOf[Extension]
      hashAndSave(targetSlotted)
      val br1 = br.updateChildAtIndex(targetSlotIdx, Some(targetSlotted.hash))
      val br2 =
        if (currentPathRemain.isEmpty) br1.updateValue(() => Some(current.value))
        else {
          val currentSlotIdx = currentPathRemain.head.value.toInt
          val currentSlotted =
            Node.leaf(currentPathRemain.drop(1), current.value).asInstanceOf[Leaf]
          hashAndSave(currentSlotted)
          br1.updateChildAtIndex(currentSlotIdx, Some(currentSlotted.hash))
        }
      hashAndSave(br2)
      val newTarget = Node.extension(maxPrefix, br2.hash)
      hashAndSave(newTarget)
    }

  }

  def combineExtensionWithExtension(target: Extension, current: Extension): Node = {
    logger.debug(s"$target <- $current")

    val targetPath  = target.encodedPath.unprefixedWithExtension
    val currentPath = current.encodedPath.unprefixedWithExtension
    if (targetPath == currentPath) {
      //todo may delete target
      target.copy(key = current.key)
    } else {
      val maxPrefix         = findMaxPrefix(targetPath, currentPath)
      val targetPathRemain  = targetPath.drop(maxPrefix.size)
      val currentPathRemain = currentPath.drop(maxPrefix.size)
      //create a branch to hold remains
      val br = Node.emptyBranch().asInstanceOf[Branch]
      //todo may delete target
      val targetRemain = Node.extension(targetPathRemain, target.key).asInstanceOf[Extension]
      hashAndSave(targetRemain)
      //todo may delete current
      val currentRemain = Node.extension(currentPathRemain, current.key).asInstanceOf[Extension]
      hashAndSave(currentRemain)
      val br1 = combineBranchWithExtension(br, targetRemain)
      val br2 = combineBranchWithExtension(br1, currentRemain)
      hashAndSave(br2)
      val newTarget = Node.extension(maxPrefix, br2.hash)
      hashAndSave(newTarget)
    }
  }

  def combineBranchWithExtension(target: Branch, current: Extension): Branch = {
    logger.debug(s"$target <- $current")

    val currentPath = current.encodedPath.unprefixedWithExtension
    if (currentPath.isEmpty) {
      // load the referred
      store.load(current.key).flatMap(serializer.fromBytes) match {
        case None =>
          throw new IllegalStateException(s"can't load referred node with hash(${current.key})")
        case Some(referred) =>
          //println(s"$referred")
          throw new java.lang.UnsupportedOperationException("should be implemented")
      }
    } else {
      val slotIdx = currentPath.head.value.toInt

      val newExtension = Node.extension(currentPath.drop(1), current.key).asInstanceOf[Extension]
      hashAndSave(newExtension)

      target.children(slotIdx) match {
        case Some(hash) =>
          store.load(hash).flatMap(serializer.fromBytes) match {
            case None =>
              throw new IllegalStateException(s"can't load referred node with hash($hash)")
            case Some(x @ Branch(_, _)) =>
              val newSlotContent = combineBranchWithExtension(x, newExtension)
              hashAndSave(newSlotContent)
              val newTarget = target.updateChildAtIndex(slotIdx, Some(newSlotContent.hash))
              hashAndSave(newTarget).asInstanceOf[Branch]
            case Some(x @ Leaf(_, _)) =>
              val newSlotContent = combineLeafWithExtension(x, newExtension)
              hashAndSave(newSlotContent)
              val newTarget = target.updateChildAtIndex(slotIdx, Some(newSlotContent.hash))
              hashAndSave(newTarget).asInstanceOf[Branch]
            case Some(x @ Extension(_, _)) =>
              val newSlotContent = combineExtensionWithExtension(x, newExtension)
              hashAndSave(newSlotContent)
              val newTarget = target.updateChildAtIndex(slotIdx, Some(newSlotContent.hash))
              hashAndSave(newTarget).asInstanceOf[Branch]
            case Some(Empty) => throw new IllegalStateException(s"impossible")
          }

        case None =>
          val newTarget = target.updateChildAtIndex(slotIdx, Some(newExtension.hash))
          hashAndSave(newTarget).asInstanceOf[Branch]
      }
    }
  }

  private def findMaxPrefix(seq1: Nibble.Sequence, seq2: Nibble.Sequence): Nibble.Sequence = {
    def find0(s1: Nibble.Sequence, s2: Nibble.Sequence, acc: Nibble.Sequence): Nibble.Sequence = {
      if (s1.isEmpty || s2.isEmpty || s1.head != s2.head) acc
      else find0(s1.drop(1), s2.drop(1), acc :+ s1.head)
    }
    find0(seq1, seq2, Nibble.Sequence.empty)
  }
}
