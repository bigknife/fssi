package fssi.store.mpt

import fssi.store.mpt.Path.{ExtensionPath, LeafPath}

sealed trait Node {}

object Node {
  case object Null                                         extends Node
  case class Leaf(path: LeafPath, data: Data)              extends Node
  case class Extension(path: ExtensionPath, address: Hash) extends Node
  case class Branch(slot: Slot, data: Data)                extends Node
}
