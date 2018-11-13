package fssi
package types
package base

/** world state is a term to indicate the determined local store state
  */
case class HashState(value: Array[Byte]) extends AnyVal {
  def ===(that: HashState): Boolean = value sameElements that.value

}

object HashState {
  /** create an empty world state, chaos
    */
  def empty: HashState = HashState(Array.emptyByteArray)

  trait Implicits {
    implicit def worldStateToBytesValue(a: HashState): Array[Byte] = a.value
  }
}
