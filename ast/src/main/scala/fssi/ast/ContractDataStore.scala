package fssi
package ast

import types._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import java.io._

@sp trait ContractDataStore[F[_]] {

  /** initialize a data directory to be a contract data store
    * @param dataDir directory to save contract data.
    */
  def initializeContractDataStore(dataDir: File): P[F, Unit]

  /** self test for a contract data store
    * @param block contract data store should be tested on block
    * @return if the store is sane return true, or false
    */
  def testContractDataStore(block: Block): P[F, Boolean]

  /** get current contract data store state
    * this state should identify current state of contract data store
    */
  def getContractDataStoreState(): P[F, HexString]

  /** verify current state of contract store
    */
  def verifyContractDataStoreState(state: String): P[F, Boolean]
}
