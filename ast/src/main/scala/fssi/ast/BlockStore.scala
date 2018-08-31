package fssi
package ast

import types._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import java.io._

@sp trait BlockStore[F[_]] {

  /** initialize a data directory to be a block store
    * @param dataDir directory to save block.
    */
  def initializeBlockStore(dataDir: File): P[F, Unit]

  /** save block, before saving, invoker should guarantee that the block is legal
    * which means, block has correct hashes, correct height etc.
    * @param block a legal block.
    */
  def saveBlock(block: Block): P[F, Unit]

  /** get latest saved block, which block must have been gone through consensus
    */
  def getLatestDeterminedBlock(): P[F, Block]

  /** get current undetermined block, and append a transaction into it, which is on consensus
    * @param determinedBlock the latest determined block which current undetermined block is based on.
    */
  def appendTransactionToUnDeterminedBlock(determinedBlock: Block, transaction: Transaction): P[F, Block]

  /** when some block is agreed, the undetermined block should be cleared, for next block to use it
    * @param block current reached agreement block
    */
  def cleanUndeterminedBlock(block: Block): P[F, Unit]
}
