package fssi
package ast

import types._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

@sp trait Ledger[F[_]] {
  /** create the genesis block for a chain
    * @param chainID the chain identity
    * @return genesis block
    */
  def createGenesisBlock(chainID: String): P[F, Block]
}
