package fssi.parser

trait Parsers[ParseError, Parser[+ _]] { self =>
  def run[A](p: Parser[A])(input: String): Either[ParseError, A]

  implicit def string(s: String): Parser[String]
  def slice[A](p: Parser[A]): Parser[String]
  def or[A](p1: Parser[A], p2: Parser[A]): Parser[A]
  def succeed[A](a: A): Parser[A]
  def map[A, B](p: Parser[A])(f: A => B): Parser[B]
  def product[A, B](p1: Parser[A], p2: Parser[B]): Parser[(A, B)]

  def char(c: Char): Parser[Char] = map(string(c.toString))(_.charAt(0))
  //def listOfN[A](n: Int, p: Parser[A]): Parser[List[A]]

  implicit def toOps[A](a: A)(implicit f: A => Parser[A]): Ops[A] = new Ops(f(a))
  implicit final class Ops[A](p: Parser[A]) {
    def or(p2: Parser[A]): Parser[A] = self.or(p, p2)
    def |(p2: Parser[A]): Parser[A] = self.or(p, p2)
  }
}

object Parsers {
  case class Law[ParseError, Parser[+ _]](parsers: Parsers[ParseError, Parser]) {
    import parsers._
    def charRule(c: Char): Boolean = run(char(c))(c.toString) == Right(c)
    def stringRule(s: String): Boolean = run(string(s))(s) == Right(s)

  }
}
