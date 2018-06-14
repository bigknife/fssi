package fssi.consensus.scp.ast.domain.types

case class Value(bytes: Array[Byte])

object Value {
  val Empty: Value = Value(Array.emptyByteArray)

  sealed trait ValidationLevel
  object ValidationLevel {
    case object InvalidValue extends ValidationLevel {
      override def toString: String = "InvalidValue"
    }
    case object FullyValidatedValue extends ValidationLevel {
      override def toString: String = "FullyValidatedValue"
    }
    case object MaybeValidValue extends ValidationLevel {
      override def toString: String = "MaybeValidValue"
    }
  }

  def invalid: ValidationLevel = ValidationLevel.InvalidValue
  def fullyValid: ValidationLevel = ValidationLevel.FullyValidatedValue
  def maybeValid: ValidationLevel = ValidationLevel.MaybeValidValue
}
