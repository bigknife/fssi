package fssi.store.mpt

import org.bouncycastle.jcajce.provider.digest.SHA3

sealed trait Hash {
  def bytes: Array[Byte]

  def isEmpty: Boolean = bytes.length == 0
  def nonEmpty: Boolean = bytes.length != 0
}

object Hash {
  def encode(source: Array[Byte]): Hash = new Hash {
    override def bytes: Array[Byte] = new SHA3.Digest256().digest(source)
  }
}
