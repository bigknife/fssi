package fssi
package ast

import types._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

@sp trait ConsensusEngine[F[_]] {
  /** initialize consensus engine
    */
  def initialize(account: Account): P[F, Unit]
}
