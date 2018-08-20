package fssi
package ast
package uc

import utils._
import types._, exception._
import types.syntax._
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

  def failM[A <: Throwable](condition: Boolean, falseException : => A): SP[F, Unit] =
    ifM(condition, (), falseException)
}
