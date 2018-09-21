package fssi.types
package biz

import base._

/**
  * Token is the measure of value
  */
case class Token(amount: BigInt, tokenUnit: Token.Unit) {
  override def toString: String = s"$amount${tokenUnit.toString}"

  def +(that: Token): Token = {
    // now, only one unit
    Token(this.amount + that.amount, tokenUnit)
  }

  def -(that: Token): Token = {
    Token(this.amount - that.amount, tokenUnit)
  }
}

object Token {
  trait Implicits {
    implicit val bizTokenOrdering: Ordering[Token] = new Ordering[Token] {
      def compare(t1: Token, t2: Token): Int = {
        // todo: consider tokenUnit
        (t1.amount - t2.amount).toInt
      }
    }

    implicit def tokenToBytesValue(a: Token): Array[Byte] = a.toString.getBytes
  }

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

  private val TokenMatcher = """^(\d+)(Sweet)""".r
  def parse(litteral: String): Token = litteral match {
    case TokenMatcher(amount, tokenUnit) => Token(amount.toInt, Unit(tokenUnit))
    case _ => Token.Zero
  }
}
