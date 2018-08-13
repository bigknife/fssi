package fssi.utils

import org.bouncycastle.jcajce.provider.digest.SHA3

trait CryptoUtil {
  def hash(source: Array[Byte]): Array[Byte] = sha3(source)

  def sha3(source: Array[Byte]): Array[Byte] =
        new SHA3.Digest256().digest(source)
}
