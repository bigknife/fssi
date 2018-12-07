package fssi.types
package base

/** unique name data
  */
case class UniqueName(value: Array[Byte]) extends AnyVal {
  def ===(other: UniqueName): Boolean = value sameElements other.value
}

object UniqueName {
  def empty: UniqueName = UniqueName(Array.emptyByteArray)
  def randomUUID(withSeperator: Boolean = true): UniqueName = {
    val s =
      if (withSeperator) java.util.UUID.randomUUID.toString
      else java.util.UUID.randomUUID.toString.replace("-", "")
    UniqueName(s)
  }
  def apply(s: String): UniqueName = UniqueName(s.getBytes("utf-8"))

  trait Implicits {
    implicit def uniqueNameToBytesValue(x: UniqueName): Array[Byte] = x.value
  }
}
