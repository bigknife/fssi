package fssi.scp.types

trait Value extends Ordered[Value]

object Value {
  sealed trait Validity
  object Validity {
    case object FullyValidated extends Validity
    case object Invalid        extends Validity
    case object MaybeValid     extends Validity
  }
}
