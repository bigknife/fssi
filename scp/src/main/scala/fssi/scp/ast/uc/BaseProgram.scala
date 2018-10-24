package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait BaseProgram[F[_]] {
  private[uc] val model: Model[F]

  private[uc] def ifM[A](cond: SP[F, Boolean], right: => SP[F, A])(left: => SP[F, A]): SP[F, A] =
    for {
      c <- cond
      a <- if (c) right else left
    } yield a

  
  private[uc] def ifM[A](cond: Boolean, right: => A)(left: => SP[F, A]): SP[F, A] =
    ifM(cond.pureSP[F], right.pureSP[F])(left)

  

  private[uc] def ifThen(cond: SP[F, Boolean])(_then: => SP[F, Unit]): SP[F, Unit] =
    for {
      c <- cond
      _ <- if (c) _then else ().pureSP[F]
    } yield ()

  private[uc] def ifThen(cond: Boolean)(_then: => SP[F, Unit]): SP[F, Unit] =
    ifThen(cond.pureSP[F])(_then)
}
