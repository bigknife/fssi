package fssi.types

import fssi.utils._

case class Base64String(bytes: Array[Byte]){
  override def toString(): String = BytesUtil.toBase64(bytes)
}
