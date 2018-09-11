package fssi
package ast

import contract.lib._
import types._, exception._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import utils._

import java.io._

@sp trait ContractService[F[_]] {
  /** check current contract running environment
    */
  def checkRunningEnvironment(): P[F, Either[FSSIException, Unit]]

  /** check the smart contract project to see where it is full-deterministic or not
    */
  def checkDeterminismOfContractProject(rootPath: File): P[F, Either[FSSIException, Unit]]

  /** compile smart contract project and output to the target file
    */
  def compileContractProject(rootPath: File,
                             sandboxVersion: String,
                             outputFile: File): P[F, Either[FSSIException, Unit]]

  /** create a running context for some transaction
    */
  def createContextInstance(sqlStore: SqlStore,
                            kvStore: KVStore,
                            tokenQuery: TokenQuery,
                            currentAccountId: String): P[F, Context]

  /** invoke a contract
    */
  def invokeUserContract(context: Context,
                         contract: Contract.UserContract,
                         method: Contract.Method,
                         params: Contract.Parameter): P[F, Either[Throwable, Unit]]

  /** get user contract gid
    */
  def getContractGlobalIdentifiedName(contract: Contract.UserContract): P[F, String]

  /** create a user contract object from a compiled contract file
    */
  def createUserContractFromContractFile(
      account: Account,
      contractFile: File,
      contractName: UniqueName,
      contractVersion: Version): P[F, Either[FSSIException, Contract.UserContract]]

  /** measure transfer cost
    */
  def measureCostToTransfer(transferedToken: Token): P[F, Token]

  /** measure publish contract cost
    */
  def measureCostToPublishContract(publishContract: Transaction.PublishContract): P[F, Token]

  /** measure run a contract cost
    */
  def measureCostToRunContract(contract: Contract.UserContract): P[F, Token]

  /** calculate bytes of user contract for beeing singed
    */
  def calculateSingedBytesOfUserContract(userContract: Contract.UserContract): P[F, BytesValue]
}
