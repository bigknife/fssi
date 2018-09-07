package fssi.types

case class Version(value: String)

object Version {
  def empty: Version = Version("")
}
