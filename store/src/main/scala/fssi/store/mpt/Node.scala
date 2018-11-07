package fssi.store.mpt

import fssi.store.mpt.Path.{ExtensionPath, LeafPath}

sealed trait Node {}

object Node {
  case object Null                                         extends Node {
    override def toString: String = "Node(Null)"
  }
  case class Leaf(path: LeafPath, data: Data)              extends Node {
    require(path.length > 0)
    override def toString: String = s"Node.Leaf($path, <data>)"
  }
  case class Extension(path: ExtensionPath, key: Key) extends Node {
    require(path.length > 0)
    override def toString: String = s"Node.Ext($path, $key)"
  }
  case class Branch(slot: Slot, data: Data)                extends Node {
    override def toString: String = s"Node.Branch($slot, <data>)"
  }

  def empty: Node = Null
  def leaf(path: Path, data: Data): Node = Leaf(path.asLeafPath, data)
  def extension(path:Path, key: Key): Node = Extension(path.asExtensionPath, key)
  def branch(slot: Slot, data: Data): Node = Branch(slot, data)
  def emptyBranch: Node = Branch(Slot.Hex.empty, Data.empty)
}
