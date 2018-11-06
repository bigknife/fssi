package fssi.store.mpt

import fssi.store.mpt.Path.{ExtensionPath, LeafPath}

sealed trait Node {}

object Node {
  case object Null                                         extends Node
  case class Leaf(path: LeafPath, data: Data)              extends Node
  case class Extension(path: ExtensionPath, key: Key) extends Node
  case class Branch(slot: Slot, data: Data)                extends Node

  def empty: Node = Null
  def leaf(path: Path, data: Data): Node = Leaf(path.asLeafPath, data)
  def extension(path:Path, key: Key): Node = Extension(path.asExtensionPath, key)
  def branch(slot: Slot, data: Data): Node = Branch(slot, data)
}
