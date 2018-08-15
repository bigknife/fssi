package fssi.types

/**
  * Token is the measure of value
  */
case class Token(amount: BigInt, tokenUnit: TokenUnit) {
  override def toString: String = s"$amount${tokenUnit.toString}"
}

object Token {
  sealed trait Unit

  object Unit {
    // basic, primary, minimum unit
    case object Sweet extends Unit {
      override def toString: String = "Sweet"
    }

    def apply(s: String): Unit = s match {
      case x if x equalsIgnoreCase "sweet" => Sweet
      case _                               => Sweet
    }
  }

  val Zero: Token = Token(0, Unit.Sweet)

  // build with sweet
  def tokenWithBaseUnit(amount: Long): Token = Token(amount, Unit.Sweet)

  private val TokenMatcher = """^(\\d+)(Sweet)""".r
  def parse(litteral: String): Token = litteral match {
    case TokenMatcher(amount, tokenUnit) => Token(amount.toInt, Unit(tokenUnit))
    case _ => Token.Zero
  }
}
