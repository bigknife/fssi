package fssi
package types
package base

/** world state is a term to indicate the determined local store state
  */
case class WorldState(value: Array[Byte]) extends AnyVal {
  def ===(that: WorldState): Boolean = value sameElements that.value

}

object WorldState {
  /** create an empty world state, chaos
    */
  def empty: WorldState = WorldState(Array.emptyByteArray)

  trait Implicits {
    implicit def worldStateToBytesValue(a: WorldState): Array[Byte] = a.value
  }
}
