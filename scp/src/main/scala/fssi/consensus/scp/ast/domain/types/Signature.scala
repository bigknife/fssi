package fssi.consensus.scp.ast.domain.types

case class Signature(bytes: Array[Byte])

object Signature {
  val Empty: Signature = Signature(Array.emptyByteArray)
}
