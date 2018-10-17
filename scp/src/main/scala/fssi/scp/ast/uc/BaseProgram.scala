package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait BaseProgram[F[_]] {
  private[uc] val model: Model[F]
}
