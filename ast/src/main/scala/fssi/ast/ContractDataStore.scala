package fssi
package ast

import contract.lib._
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

  /** commit staged contract data
    */
  def commitStagedContractData(height: BigInt): P[F, Unit]

  /** rollback staged contract data
    */
  def rollbackStagedContractData(height: BigInt): P[F, Unit]

    /** prepare a sql store for running a specified contract
    */
  def prepareSqlStoreFor(height: BigInt, contract: Contract.UserContract): P[F, SqlStore]

  /** close a sql store
    */
  def closeSqlStore(sqlStore: SqlStore): P[F, Unit]

  /** prepare a key value store for running a specified contract
    */
  def prepareKeyValueStoreFor(height: BigInt, contract: Contract.UserContract): P[F, KVStore]

  /** close a kv store
    */
  def closeKeyValueStore(kvStore: KVStore): P[F, Unit]
}
