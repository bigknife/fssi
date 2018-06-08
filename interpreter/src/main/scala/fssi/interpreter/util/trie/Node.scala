package fssi.interpreter.util.trie

import fssi.interpreter.util._

sealed trait Node {
  def hash: Hash

  override def equals(obj: scala.Any): Boolean = obj match {
    case x: Node => x.hash sameElements this.hash
    case _       => false
  }
}
object Node {

  case object Empty extends Node {
    override def toString: String = "Empty"

    override def hash: Hash = Array.emptyByteArray
  }
  case class Branch(children: Array[Option[Hash]], value: Option[Value]) extends Node {
    def isEmpty: Boolean = !children.exists(_.isDefined)
    override def hash: Hash = {
      val childHashes =
        if (isEmpty) Array.emptyByteArray
        else children.filter(_.isDefined).map(_.get).reduce(_ ++ _)
      value match {
        case Some(v) => crypto.sha3(crypto.sha3(v) ++ childHashes)
        case None    => crypto.sha3(childHashes)
      }
    }

    override def toString: String = {
      val s = "[" + children
        .map(x => if (x.isEmpty) "-" else "*")
        .mkString(" ") + "] -> " + (if (value.isEmpty) "<Empty>" else "[Data]")
      s"Branch($s)"
    }

    def updateChildAtIndex(idx: Int, hashOpt: Option[Hash]): Branch = {
      val newChildren = children
        .foldLeft((0, Array.empty[Option[Hash]])) {
          case ((i, acc), _) if i == idx => (i + 1, acc :+ hashOpt)
          case ((i, acc), n)             => (i + 1, acc :+ n)
        }
        ._2
      Branch(newChildren, value)
    }

    def updateValue(f: () => Option[Value]): Branch = {
      copy(value = f())
    }
  }
  case class Leaf(encodedPath: Nibble.Sequence, value: Value) extends Node {
    override def toString: String = {
      val prefix = encodedPath.leafPreifx
      val key    = encodedPath.unprefixedWithLeaf
      s"Leaf($prefix,$key -> [Value])"
    }

    override def hash: Hash = crypto.sha3(value ++ encodedPath.toByteArray())
  }
  case class Extension(encodedPath: Nibble.Sequence, key: Key) extends Node {
    override def toString: String = {
      val prefix = encodedPath.extensionPrefix
      val key    = encodedPath.unprefixedWithExtension
      s"Extension($prefix,$key -> [Key])"
    }

    override def hash: Hash = crypto.sha3(key ++ encodedPath.toByteArray())
  }

  def leaf(key: Key, value: Value): Node = {
    Leaf(Nibble.Sequence(key).prefixedWithLeaf, value)
  }

  def leaf(key: Nibble.Sequence, value: Value): Node = {
    Leaf(key.prefixedWithLeaf, value)
  }

  def extension(key: Nibble.Sequence, value: Key): Node = {
    Extension(key.prefixedWithExtension, value)
  }
  def emptyBranch(): Node = {
    val arr = Array[Option[Hash]](
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None
    )
    Branch(arr, None)
  }

  trait Serializer {
    def toBytes(node: Node): Array[Byte]
    def fromBytes(bytes: Array[Byte]): Option[Node]
  }

  object Serializer {
    import fssi.ast.domain.types.BytesValue
    import BytesValue.converters._

    /** simple serializer,
      *
      */
    trait SimpleSerializer extends Serializer {
      //todo: complete serializer

      val EmptyTag: Array[Byte]     = Array[Byte](0)
      val BranchTag: Array[Byte]    = Array[Byte](1)
      val LeafTag: Array[Byte]      = Array[Byte](2)
      val ExtensionTag: Array[Byte] = Array[Byte](3)

      override def fromBytes(bytes: Array[Byte]): Option[Node] =
        if (bytes.length < 1) None
        else {
          val head = bytes.head
          head match {
            case x if EmptyTag.contains(x) => Some(Empty)

            case x if BranchTag.contains(x) && bytes.length > 9 =>
              val body                = bytes.drop(1)
              val childrenBytesLength = BytesValue(body.take(4)).convertTo[Int]
              val valueLength         = BytesValue(body.slice(4, 8)).convertTo[Int]
              val data                = body.drop(8)
              if (data.length != childrenBytesLength + valueLength) None
              else {
                val childrenBytes = data.take(childrenBytesLength)
                val valueBytes    = data.slice(childrenBytesLength, childrenBytesLength + valueLength)
                Some(
                  Branch(decodeBranchChildren(childrenBytes),
                         if (valueBytes.isEmpty) None else Some(valueBytes)))
              }

            case x if LeafTag.contains(x) && bytes.length > 9 =>
              val body        = bytes.drop(1)
              val pathLength  = BytesValue(body.take(4)).convertTo[Int]
              val valueLength = BytesValue(body.slice(4, 8)).convertTo[Int]
              val data        = body.drop(8)
              if (data.length != pathLength + valueLength) None
              else {

                Some(
                  Leaf(Nibble.Sequence.divide(data.take(pathLength)),
                       data.slice(pathLength, pathLength + valueLength)))
              }

            case x if ExtensionTag.contains(x) =>
              val body       = bytes.drop(1)
              val pathLength = BytesValue(body.take(4)).convertTo[Int]
              val keyLength  = BytesValue(body.slice(4, 8)).convertTo[Int]
              val data       = body.drop(8)
              if (data.length != pathLength + keyLength) None
              else {
                Some(
                  Extension(Nibble.Sequence.divide(data.take(pathLength)),
                            data.slice(pathLength, pathLength + keyLength)))
              }
            case _ => None
          }
        }

      override def toBytes(node: Node): Array[Byte] = node match {
        case Empty                      => EmptyTag
        case Branch(children, valueOpt) =>
          // 1Bytes(tag) 4Bytes(childrenlen) 4Bytes(valuelen) childrenbytes value
          val childrenBytes = encodeBranchChildren(children)

          val bytes = BranchTag ++ BytesValue.valueOf(childrenBytes.length).bytes ++ valueOpt
            .map(x => BytesValue.valueOf(x.length).bytes)
            .getOrElse(BytesValue.valueOf(0).bytes) ++ childrenBytes ++ valueOpt.getOrElse(
            Array.emptyByteArray)

          bytes

        case Leaf(encodedPath, value) =>
          val pathBytes = encodedPath.toByteArray()
          LeafTag ++ BytesValue.valueOf(pathBytes.length).bytes ++ BytesValue
            .valueOf(value.length)
            .bytes ++ pathBytes ++ value
        case Extension(encodedPath, key) =>
          val pathBytes = encodedPath.toByteArray()
          ExtensionTag ++ BytesValue.valueOf(pathBytes.length).bytes ++ BytesValue
            .valueOf(key.length)
            .bytes ++ pathBytes ++ key

      }

      // encode children hash: (index(nibble), hashLength, Hash)
      private def encodeBranchChildren(children: Array[Option[Hash]]): Array[Byte] = {
        children.foldLeft(Array.emptyByteArray) {
          case (arr, Some(hash)) =>
            arr ++ BytesValue.valueOf(hash.length).bytes ++ hash
          case (arr, None) =>
            arr ++ BytesValue.valueOf(0).bytes
        }
      }

      private def decodeBranchChildren(bytes: Array[Byte]): Array[Option[Hash]] = {
        def decode0(xs: Array[Byte], acc: Array[Option[Hash]]): Array[Option[Hash]] =
          if (xs.isEmpty) acc
          else {
            val length = BytesValue(xs.take(4)).convertTo[Int]
            if (length == 0) decode0(xs.drop(4), acc :+ None)
            else decode0(xs.drop(4 + length), acc :+ Some(xs.slice(4, 4 + length)))
          }

        decode0(bytes, Array.empty[Option[Hash]])
      }
    }

    implicit val simple: SimpleSerializer = new SimpleSerializer {}
  }
}
