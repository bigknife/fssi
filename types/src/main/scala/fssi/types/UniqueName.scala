package fssi.types

case class UniqueName(value: String)

object UniqueName {
  def randomUUID(withSeperator: Boolean = true): UniqueName =
    if (withSeperator) UniqueName(java.util.UUID.randomUUID.toString)
    else UniqueName(java.util.UUID.randomUUID.toString.replace("-", ""))

  def empty: UniqueName = UniqueName("")
}
