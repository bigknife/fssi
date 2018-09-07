package fssi
package parser
package sql

trait SqlParsers extends Parsers[SqlParseError, SqlParser] {
  import SqlParser._
  type Result[A] = Either[SqlParseError, A]
  override def run[A](p: SqlParser[A])(input: String): Either[SqlParseError, A] = p match {
    case PString(s) =>
      if (s == input) Right(s).asInstanceOf[Result[A]]
      else Left(SqlParseError(s"unexpected input: $input"))
    case POr(p1, p2) =>
      val v1 = run(p1)(input)
      if (v1.isLeft) run(p2)(input)
      else v1
    case PSucceed(a) => Right(a)
    case PMap(p1, f) => run(p1)(input).right.map(f)
    case PProduct(p1, p2) =>
      (for {
        v1 <- run(p1)(input).right
        v2 <- run(p2)(input).right
      } yield (v1, v2)).asInstanceOf[Result[A]]
    case PSlice(p1) =>
      run(p1)(input) match {
        case Left(_) => Right(input).asInstanceOf[Result[A]]
        case Right(value) => Right("").asInstanceOf[Result[A]]
      }


  }

  implicit override def string(s: String): SqlParser[String] = PString(s)

  override def slice[A](p: SqlParser[A]): SqlParser[String] = PSlice(p)

  override def succeed[A](a: A): SqlParser[A] = PSucceed(a)

  override def map[A, B](p: SqlParser[A])(f: A => B): SqlParser[B] = PMap(p, f)

  override def product[A, B](p1: SqlParser[A], p2: SqlParser[B]): SqlParser[(A, B)] =
    PProduct(p1, p2)

  override def or[A](p1: SqlParser[A], p2: SqlParser[A]): SqlParser[A] = POr(p1, p2)
}
