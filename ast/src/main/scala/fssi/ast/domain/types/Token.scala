package fssi.ast.domain.types

case class Token(
    amount: Long,
    unit: Token.Unit
) {
  def ordered: Ordered[Token] = Ordered.orderingToOrdered(this)

  def toBase: Token = {
    // todo other unit to base unit.
    this
  }
}

object Token {
  implicit val tokenOrderring: Ordering[Token] = new Ordering[Token] {
    override def compare(x: Token, y: Token): Int = ???
  }


  sealed trait Unit {}

  object Unit {
    // basic, primary, minimum unit
    case object Sweet extends Unit
  }

  val Zero: Token = Token(0, Unit.Sweet)

  // build with sweet
  def tokenWithBaseUnit(amount: Long): Token = Token(amount, Unit.Sweet)
}
