package fssi
package parser
package sql

sealed trait SqlParser[+A] {

}

object SqlParser {
  case class PString(value: String) extends SqlParser[String]
  case class POr[A](p1: SqlParser[A], p2: SqlParser[A]) extends SqlParser[A]
  case class PSucceed[A](a: A)extends SqlParser[A]
  case class PMap[A, B](p: SqlParser[A], f: A => B) extends SqlParser[B]
  case class PProduct[A, B](p1: SqlParser[A], p2: SqlParser[B]) extends SqlParser[(A, B)]
  case class PSlice[A](p: SqlParser[A]) extends SqlParser[String]
}
