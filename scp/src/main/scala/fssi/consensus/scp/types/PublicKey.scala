package fssi.consensus.scp.types

/**
  * A 256 bits data.
  * @param bytes 256 bits
  */
case class PublicKey(bytes: Array[Byte]) {
  // 32bytes == 256bits
  assert(bytes.length == 32)
}
