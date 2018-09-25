package fssi
package ast
package uc

import types._
import utils._
import types.syntax._
import bigknife.sop._
import bigknife.sop.implicits._

/** Tool program composited by LocalChainProgram, AccountProgram, TransactionFactoryProgram and ContractProgram
  */
trait ToolProgram[F[_]]
    extends tool.LocalChainProgram[F]
    with tool.AccountProgram[F]
    with tool.TransactionFactoryProgram[F]
    with tool.ContractProgram[F] {}

object ToolProgram {
  def apply[F[_]](implicit M: components.Model[F]): ToolProgram[F] = new ToolProgram[F] {
    val model: components.Model[F] = M
  }
}
