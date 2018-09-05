package fssi
package ast

import contract.lib._
import types._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import java.io._

@sp trait ContractStore[F[_]] {

  /** initialize a data directory to be a contract store
    * @param dataDir directory to save contract.
    */
  def initializeContractStore(dataDir: File): P[F, Unit]

  /** self test for a contract store
    * @param block contract store should be tested on block
    * @return if the store is sane return true, or false
    */
  def testContractStore(block: Block): P[F, Boolean]

  /** get current token store state
    * this state should identify current state of token store
    */
  def getTokenStoreState(): P[F, HexString]

  /** verify current state of contract store
    */
  def verifyContractStoreState(state: String): P[F, Boolean]

  /** commit staged contract
    */
  def commitStagedContract(height: BigInt): P[F, Unit]

  /** roolback staged contract
    */
  def rollbackStagedContract(height: BigInt): P[F, Unit]

  /** find user contract with gid
    */
  def findUserContract(name: UniqueName, version: Version): P[F, Option[Contract.UserContract]]

  /** prepare a sql store for running a specified contract
    */
  def prepareSqlStoreFor(height: BigInt, contract: Contract.UserContract): P[F, SqlStore]

  /** prepare a key value store for running a specified contract
    */
  def prepareKeyValueStoreFor(height: BigInt, contract: Contract.UserContract): P[F, KVStore]
}
