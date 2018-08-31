package fssi.types

case class Signature(value: HexString)

object Signature {
  def empty: Signature = Signature(HexString.empty)
}
