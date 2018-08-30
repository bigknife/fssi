package fssi
package ast

import types._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

@sp trait ConsensusEngine[F[_]] {
  /** initialize consensus engine
    */
  def initializeConsensusEngine(account: Account): P[F, Unit]

  /** try to agree a new block 
    * @param account the consensus procedure initiator
    * @param previous the previous block, latest determined block
    * @param agreeing current block, being in consensus procedure
    */
  def tryToAgreeBlock(account: Account, previous: Block, agreeing: Block): P[F, Unit]

  /** handle consensus-special message
    */
  def handleConsensusAuxMessage(account: Account, auxMessage: ConsensusAuxMessage): P[F, Unit]
}
