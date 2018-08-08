package fssi
package interpreter
package util

import org.bouncycastle.jcajce.provider.digest.SHA3

trait CryptoUtil {
  def hash(source: Array[Byte]): Array[Byte] =
    new SHA3.Digest256().digest(source)
}
