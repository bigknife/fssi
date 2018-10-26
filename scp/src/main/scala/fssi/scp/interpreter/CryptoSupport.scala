package fssi.scp
package interpreter

import java.math.BigInteger

import fssi.utils._

trait CryptoSupport {
  def computeHashValue(bytes: Array[Byte]): BigInt = {
    val hash = crypto.sha256(bytes)
    BigInt(new BigInteger(1,  hash.take(8).reverse))
  }
}
