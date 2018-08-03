package fssi.types

case class Node(
  host: String,
  port: Int,
  account: Option[Account]
)
