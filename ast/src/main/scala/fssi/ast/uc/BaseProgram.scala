package fssi
package ast
package uc

import utils._
import types._, exception._
import bigknife.sop._
import bigknife.sop.implicits._

trait BaseProgram[F[_]] {
  val model: components.Model[F]

  /** ifM, abbr for err.either
    */
  def ifM[A](condition: Boolean, trueValue: => A, falseException: => Throwable): SP[F, A] =
    model.err.either(
      Either.cond(condition, trueValue, falseException)
    )

  /** requireM throws exception when condition is false
    */
  def requireM[A <: FSSIException](condition: Boolean, falseException: => A): SP[F, Unit] =
    ifM(condition, (), falseException)

  implicit final class SPEitherOps[A, E <: Throwable](x: SP[F, Either[E, A]]) {
    def right: SP[F, A] =
      for {
        ax <- x
        a  <- model.err.either(ax)
      } yield a
  }
  implicit def toSPEitherOps[A, E <: Throwable](x: P[F, Either[E, A]]): SPEitherOps[A, E] =
    new SPEitherOps(x: SP[F, Either[E, A]])
}
