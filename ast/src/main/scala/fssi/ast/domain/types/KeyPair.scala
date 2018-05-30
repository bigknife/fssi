package fssi.ast.domain.types

case class KeyPair(
    priv: KeyPair.Priv,
    publ: KeyPair.Publ
)

object KeyPair {
  case class Priv(bytes: Array[Byte]) extends BytesValue
  case class Publ(bytes: Array[Byte]) extends BytesValue
}
