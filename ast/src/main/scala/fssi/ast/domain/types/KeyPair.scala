package fssi.ast.domain.types

import java.security.PrivateKey
import java.security.PublicKey

case class KeyPair(
    priv: KeyPair.Priv,
    publ: KeyPair.Publ
)

object KeyPair {
  case class Priv(privateKey: PrivateKey) extends BytesValue {
    def bytes: Array[Byte] = privateKey.getEncoded
  }
  case class Publ(publicKey: PublicKey) extends BytesValue {
    def bytes: Array[Byte] = publicKey.getEncoded
  }

  def fromJCEKeyPair(kp: java.security.KeyPair): KeyPair =
    KeyPair(Priv(kp.getPrivate), Publ(kp.getPublic))


}
