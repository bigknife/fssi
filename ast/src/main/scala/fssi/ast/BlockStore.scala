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
  def initialize(dataDir: File): P[F, Unit]

  /** save block, before saving, invoker should guarantee that the block is legal
    * which means, block has correct hashes, correct height etc.
    * @param block a legal block.
    */
  def saveBlock(block: Block): P[F, Block]
}
