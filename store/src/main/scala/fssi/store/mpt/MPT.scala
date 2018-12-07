package fssi.store.mpt

import fssi.store.core.KVStore
import org.slf4j.LoggerFactory

trait MPT {

  private[mpt] def store: KVStore
  private[mpt] lazy val log = LoggerFactory.getLogger(getClass)

  def resolveStoreName(key: Array[Byte]): String
  def combineStoreNameAndKey(storeName: String, key: Array[Byte]): Array[Byte]

  // user's method, put
  def put(k: Array[Byte], v: Array[Byte]): Either[Throwable, Unit] = {
    val hash = Hash.encode(k)
    val name = resolveStoreName(k)
    val key  = Key.encode(hash)
    store.transact { proxy =>
      insertTrie(name, key, Data.wrap(v), proxy)
    }
  }
  // user's method, get
  def get(k: Array[Byte]): Either[Throwable, Option[Array[Byte]]] = {
    val hash = Hash.encode(k)
    val name = resolveStoreName(k)
    val key  = Key.encode(hash)
    store.transact { proxy =>
      fetchTrie(name, key, proxy).map(_.bytes)
    }
  }
  // root hash
  def rootKey(name: String): Option[Key] = {
    store
      .transact { proxy =>
        for {
          key <- proxy.get(combineStoreNameAndKey(name, name.getBytes("utf-8"))).map(Key.wrap)
        } yield key
      }
      .toOption
      .flatten
  }
  def rootHash(name: String): Option[Hash] = {
    rootKey(name)
      .map(x => Hash.wrap(x.bytes))
  }

  def rootNode(name: String): Option[Node] = {
    store
      .transact { proxy =>
        rootNode(name, proxy)
      }
      .right
      .map(_.map(_._1))
      .toOption
      .flatten
  }

  trait Proxy {
    def put(k: Array[Byte], v: Array[Byte]): Unit
    def get(k: Array[Byte]): Option[Array[Byte]]
    def clean(k: Array[Byte]): Unit
    def rootHash(name: String): Option[Hash]
  }

  def transact[A](f: Proxy => A): Either[Throwable, A] = {
    store.transact { proxy =>
      val p = new Proxy {
        override def put(k: Array[Byte], v: Array[Byte]): Unit = {
          val hash = Hash.encode(k)
          val name = resolveStoreName(k)
          val key  = Key.encode(hash)
          insertTrie(name, key, Data.wrap(v), proxy)
        }
        override def get(k: Array[Byte]): Option[Array[Byte]] = {
          val hash = Hash.encode(k)
          val name = resolveStoreName(k)
          val key  = Key.encode(hash)
          fetchTrie(name, key, proxy).map(_.bytes)
        }
        override def clean(k: Array[Byte]): Unit = {
          val name = resolveStoreName(k)
          proxy.clean(name)
        }

        override def rootHash(name: String): Option[Hash] = {
          for {
            key <- proxy.get(combineStoreNameAndKey(name, name.getBytes("utf-8"))).map(Key.wrap)
          } yield Hash.wrap(key.bytes)
        }
      }
      f(p)
    }
  }

  private def insertTrie(name: String, key: Key, data: Data, proxy: KVStore#Proxy): Unit = {
    val (n, k) = rootNode(name, proxy).getOrElse((Node.Null, Key.empty))
    val leaf   = Node.leaf(key.toPath, data)
    if (k.nonEmpty) {
      proxy.delete(combineStoreNameAndKey(name, k.bytes))
    }
    val (_, newRootKey) = saveNode(name, combineNode(name, leaf, n, proxy), proxy)
    // set new root node
    proxy.put(combineStoreNameAndKey(name, name.getBytes("utf-8")), newRootKey.bytes)
    log.debug(s"update new root: $name -> ${new String(newRootKey.bytes, "utf-8")}")
  }

  private def fetchTrie(name: String, key: Key, proxy: KVStore#Proxy): Option[Data] = {

    def _fetch(path: Path, node: Node): Option[Data] = node match {
      case Node.Null                            => None
      case Node.Leaf(p, data) if path === p     => Some(data)
      case Node.Leaf(_, _)                      => None
      case Node.Branch(_, data) if path.isEmpty => Some(data)
      case Node.Branch(slot, _) =>
        val cell = slot.get(path.head.toInt)
        if (cell.key.isEmpty) None
        else {
          val next = for {
            data <- proxy.get(combineStoreNameAndKey(name, cell.key.bytes)).map(Data.wrap)
            n    <- data.toNode
          } yield n
          if (next.isEmpty) None
          else _fetch(path.dropHead, next.get)
        }
      case Node.Extension(p, k) if p.length == 0 && path.length == 0 =>
        val next = for {
          data <- store.get(k.bytes).map(Data.wrap)
          n    <- data.toNode
        } yield n
        if (next.isEmpty) None
        else _fetch(path, next.get)
      case Node.Extension(p, _) if !p.hasPrefix(path) => None
      case Node.Extension(p, k) =>
        val prefix = p.prefix(path)
        if (p.length > prefix.length) {
          _fetch(path.drop(prefix), Node.extension(p.drop(prefix), k))
        } else {
          val nextNode = for {
            data <- proxy.get(combineStoreNameAndKey(name, k.bytes)).map(Data.wrap)
            n    <- data.toNode
          } yield n
          nextNode match {
            case None => None
            case Some(n) =>
              _fetch(path.drop(prefix), n)
          }
        }

    }

    val n = rootNode(name, proxy)
    n.flatMap(x => _fetch(Path.plain(key.bytes), x._1)) match {
      case Some(x) => Some(x)
      case None =>
        log.warn(s"not found data for key: ${new String(key.bytes, "utf-8")}")
        None
    }

  }

  private def combineNode(name: String, node1: Node, node2: Node, proxy: KVStore#Proxy): Node = {
    log.debug(s"combine $node1 to $node2")
    (node1, node2) match {
      case (Node.Null, Node.Null) => Node.Null

      case (x, Node.Null) => x
      case (Node.Null, x) => x

      case (x @ Node.Leaf(_, _), y @ Node.Leaf(_, _))           => combineNode(name, x, y, proxy)
      case (x @ Node.Branch(_, _), y @ Node.Branch(_, _))       => combineNode(name, x, y, proxy)
      case (x @ Node.Extension(_, _), y @ Node.Extension(_, _)) => combineNode(name, x, y, proxy)

      case (x @ Node.Leaf(_, _), y @ Node.Branch(_, _))    => combineNode(name, x, y, proxy)
      case (x @ Node.Leaf(_, _), y @ Node.Extension(_, _)) => combineNode(name, x, y, proxy)

      case (x @ Node.Branch(_, _), y @ Node.Leaf(_, _))      => combineNode(name, y, x, proxy)
      case (x @ Node.Branch(_, _), y @ Node.Extension(_, _)) => combineNode(name, x, y, proxy)

      case (x @ Node.Extension(_, _), y @ Node.Branch(_, _)) => combineNode(name, x, y, proxy)
      case (x @ Node.Extension(_, _), y @ Node.Leaf(_, _))   => combineNode(name, y, x, proxy)

    }
  }
  private def rootNode(name: String, proxy: KVStore#Proxy): Option[(Node, Key)] = {
    for {
      rootKey      <- proxy.get(combineStoreNameAndKey(name, name.getBytes("utf-8")))
      rootNodeData <- proxy.get(combineStoreNameAndKey(name, rootKey)).map(Data.wrap)
      rootNode     <- rootNodeData.toNode
    } yield (rootNode, Key.wrap(rootKey))
  }

  private def saveNode[N <: Node](name: String, node: N, proxy: KVStore#Proxy): (N, Key) = {
    val data = Data.encode(node)
    val key  = Key.encode(data.hash)
    proxy.put(combineStoreNameAndKey(name, key.bytes), data.bytes)
    (node, key)
  }

  private def combineNode(name: String,
                          node1: Node.Leaf,
                          node2: Node.Leaf,
                          proxy: KVStore#Proxy): Node = {
    if (node1.path === node2.path) node1
    else {
      val prefix = node1.path.prefix(node2.path)
      if (prefix.length != 0) {
        val branchData =
          if (prefix === node1.path) node1.data
          else if (prefix === node2.path) node2.data
          else Data.empty

        val branch = {
          val br = Node.branch(Slot.Hex.empty, branchData)

          val br1 =
            if (prefix === node1.path) br
            else {
              val leaf = Node.leaf(node1.path.drop(prefix), node1.data)
              combineNode(name, leaf, br, proxy)
            }

          val br2 =
            if (prefix == node2.path) br1
            else {
              val leaf = Node.leaf(node2.path.drop(prefix), node2.data)
              combineNode(name, leaf, br1, proxy)
            }
          br2
        }
        val (_, branchKey) = saveNode(name, branch, proxy)
        Node.extension(prefix, branchKey)
      } else {
        val br0 = Node.emptyBranch
        val br1 = combineNode(name, node1, br0, proxy)
        combineNode(name, node2, br1, proxy)
      }
    }
  }

  private def combineNode(name: String,
                          node1: Node.Leaf,
                          node2: Node.Branch,
                          proxy: KVStore#Proxy): Node = {
    val node1head = node1.path.head
    val slotCell  = node2.slot.get(node1head.toInt)
    if (slotCell.key.isEmpty) {
      if (node1.path.length > 1) {
        // if the length > 1, we can build a leaf node
        val (_, leafKey) = saveNode(name, Node.leaf(node1.path.dropHead, node1.data), proxy)
        val newSlot      = node2.slot.update(node1head, leafKey)
        Node.branch(newSlot, node2.data)
      } else {
        // if the length == 1, we have to build a empty branch node with data = node1.data
        val (_, branchHash) = saveNode(name, Node.branch(Slot.Hex.empty, node1.data), proxy)
        val newSlot         = node2.slot.update(node1head, branchHash)
        Node.branch(newSlot, node2.data)
      }
    } else {
      if (node1.path.length > 1) {
        // if the length > 1 we can build a leaf node , then combine it to old node
        val leaf = Node.leaf(node1.path.dropHead, node1.data)
        val old = for {
          data <- proxy.get(combineStoreNameAndKey(name, slotCell.key.bytes)).map(Data.wrap)
          node <- data.toNode
        } yield node
        proxy.delete(combineStoreNameAndKey(name, slotCell.key.bytes))
        val (_, newSlotKey) = saveNode(name, combineNode(name, leaf, old.get, proxy), proxy)
        val newSlot         = node2.slot.update(node1head, newSlotKey)
        Node.branch(newSlot, node2.data)
      } else {
        // if the length == 1, we have to build a empty branch node with data = node1.data, and combine old to this one
        val old = for {
          data <- proxy.get(combineStoreNameAndKey(name, slotCell.key.bytes)).map(Data.wrap)
          node <- data.toNode
        } yield node

        val branch = Node.branch(Slot.Hex.empty, node1.data)
        proxy.delete(combineStoreNameAndKey(name, slotCell.key.bytes))
        val (_, newBranchKey) = saveNode(name, combineNode(name, old.get, branch, proxy), proxy)
        val newSlot           = node2.slot.update(node1head, newBranchKey)
        Node.branch(newSlot, node2.data)
      }
    }
  }
  private def combineNode(name: String,
                          node1: Node.Leaf,
                          node2: Node.Extension,
                          proxy: KVStore#Proxy): Node = {
    if (node1.path === node2.path) node1
    else {
      val prefix = node1.path.prefix(node2.path)
      if (prefix.length == 0) {
        // no common prefix, build a empty branch, combine them to it
        val branch = Node.branch(Slot.Hex.empty, Data.empty)
        val br1    = combineNode(name, node1, branch, proxy)
        combineNode(name, node2, br1, proxy)
      } else {
        val n1remainPath = node1.path.drop(prefix)
        val n2remainPath = node2.path.drop(prefix)
        if (n1remainPath.nonEmpty && n2remainPath.nonEmpty) {
          val br0        = Node.branch(Slot.Hex.empty, Data.empty)
          val leaf       = Node.leaf(n1remainPath, node1.data)
          val br1        = combineNode(name, leaf, br0, proxy)
          val ext        = Node.extension(n2remainPath, node2.key)
          val (_, brKey) = saveNode(name, combineNode(name, ext, br1, proxy), proxy)
          // return a extension node with key referring to brKey
          Node.extension(prefix, brKey)
        } else if (n1remainPath.isEmpty) {
          val br0        = Node.branch(Slot.Hex.empty, node1.data)
          val ext        = Node.extension(n2remainPath, node2.key)
          val (_, brKey) = saveNode(name, combineNode(name, ext, br0, proxy), proxy)
          // return a extension node with key referring to brKey
          Node.extension(prefix, brKey)
        } else {
          //n2remainPath is empty
          val leaf = Node.leaf(n1remainPath, node1.data)
          val old = for {
            data <- proxy.get(combineStoreNameAndKey(name, node2.key.bytes)).map(Data.wrap)
            node <- data.toNode
          } yield node
          proxy.delete(combineStoreNameAndKey(name, node2.key.bytes))
          val (_, newKey) = saveNode(name, combineNode(name, leaf, old.get, proxy), proxy)
          node2.copy(key = newKey)
        }
      }
    }
  }

  private def combineNode(name: String,
                          node1: Node.Extension,
                          node2: Node.Branch,
                          proxy: KVStore#Proxy): Node = {
    val node1head = node1.path.head
    val cell      = node2.slot.get(node1head.toInt)
    if (cell.key.isEmpty) {
      if (node1.path.length > 1) {
        val (_, newExtKey) = saveNode(name, Node.extension(node1.path.dropHead, node1.key), proxy)
        val newSlot        = node2.slot.update(node1head, newExtKey)
        Node.branch(newSlot, node2.data)
      } else {
        // only head, so simply update the cell's key
        val newSlot = node2.slot.update(node1head, node1.key)
        Node.branch(newSlot, node2.data)
      }
    } else {
      // cell not empty
      val oldBrCellRef = for {
        data <- proxy.get(combineStoreNameAndKey(name, cell.key.bytes)).map(Data.wrap)
        node <- data.toNode
      } yield node
      //delete old
      proxy.delete(combineStoreNameAndKey(name, cell.key.bytes))
      if (node1.path.length > 1) {
        val (_, newNodeKey) = saveNode(name,
                                       combineNode(name,
                                                   Node.extension(node1.path.dropHead, node1.key),
                                                   oldBrCellRef.get,
                                                   proxy),
                                       proxy)
        val newSlot = node2.slot.update(node1head, newNodeKey)
        Node.branch(newSlot, node2.data)
      } else {
        // only head, so load the node, and combine to br's cell key referring node
        val oldExtRef = for {
          data <- proxy.get(combineStoreNameAndKey(name, node1.key.bytes)).map(Data.wrap)
          node <- data.toNode
        } yield node
        proxy.delete(combineStoreNameAndKey(name, node1.key.bytes))
        val (_, newNodeKey) =
          saveNode(name, combineNode(name, oldExtRef.get, oldBrCellRef.get, proxy), proxy)
        val newSlot = node2.slot.update(node1head, newNodeKey)
        Node.branch(newSlot, node2.data)
      }
    }
  }

  private def combineNode(name: String,
                          node1: Node.Extension,
                          node2: Node.Extension,
                          proxy: KVStore#Proxy): Node = {
    if (node1.path === node2.path) node1
    else {
      val prefix = node1.path.prefix(node2.path)
      if (prefix.length == 0) {
        val br0 = Node.branch(Slot.Hex.empty, Data.empty)
        val br1 = combineNode(name, node1, br0, proxy)
        combineNode(name, node2, br1, proxy)
      } else {
        val n1remainPath = node1.path.drop(prefix)
        val n2remainPath = node2.path.drop(prefix)
        if (n1remainPath.isEmpty) {
          val ext = Node.extension(n2remainPath, node2.key)
          val old = for {
            data <- proxy.get(combineStoreNameAndKey(name, node1.key.bytes)).map(Data.wrap)
            node <- data.toNode
          } yield node
          proxy.delete(combineStoreNameAndKey(name, node1.key.bytes))
          combineNode(name, ext, old.get, proxy)
        } else if (n2remainPath.isEmpty) {
          val ext = Node.extension(n1remainPath, node1.key)
          val old = for {
            data <- proxy.get(combineStoreNameAndKey(name, node2.key.bytes)).map(Data.wrap)
            node <- data.toNode
          } yield node
          proxy.delete(combineStoreNameAndKey(name, node2.key.bytes))
          combineNode(name, ext, old.get, proxy)
        } else {
          val br0  = Node.branch(Slot.Hex.empty, Data.empty)
          val ext1 = Node.extension(n1remainPath, node1.key)
          val br1  = combineNode(name, ext1, br0, proxy)
          val ext2 = Node.extension(n2remainPath, node2.key)
          combineNode(name, ext2, br1, proxy)
        }
      }
    }
  }
  private def combineNode(name: String,
                          node1: Node.Branch,
                          node2: Node.Branch,
                          proxy: KVStore#Proxy): Node = {
    throw new RuntimeException("branch node can't be combined with another branch node")
  }

}

object MPT {
  def apply(
      storeNameResolver: Array[Byte] => String,
      storeKeyCombinator: (String, Array[Byte]) => Array[Byte])(implicit kvStore: KVStore): MPT =
    new MPT {
      override private[mpt] def store                         = kvStore
      override def resolveStoreName(key: Array[Byte]): String = storeNameResolver(key)
      override def combineStoreNameAndKey(storeName: String, key: Array[Byte]): Array[Byte] =
        storeKeyCombinator(storeName, key)
    }
}
