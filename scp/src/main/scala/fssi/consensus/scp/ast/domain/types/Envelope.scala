package fssi.consensus.scp.ast.domain.types

case class Envelope(
    statement: Statement,
    signature: Signature
)

object Envelope {
  sealed trait State
  object State {
    case object Invalid extends State {
      override def toString: String = "Invalid"
    }
    case object Valid extends State {
      override def toString: String = "Valid"
    }
  }
}
