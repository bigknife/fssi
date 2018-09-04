package fssi
package ast

import contract.lib._
import types._, exception._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._


import java.io._

@sp trait ContractService[F[_]] {

  /** check the smart contract project to see where it is full-deterministic or not
    */
  def checkDeterminismOfContractProject(rootPath: File): P[F, Either[FSSIException, Unit]]

  /** compile smart contract project and output to the target file
    */
  def compileContractProject(rootPath: File,
                             sandboxVersion: String,
                             outputFile: File): P[F, Either[FSSIException, Unit]]

  /** resolve contract gid
    */
  def resolveContractGlobalIdentifiedName(name: UniqueName, version: Version): P[F, String]

  /** create context for a transaction in current block (height)
    */
  def createContractRunningContextInstance(height: BigInt,
                                           transaction: Transaction.RunContract): P[F, Context]

  /** run smart contract
    * 
    */
  def invokeContract(contract: Contract.UserContract,
                  method: Contract.Method,
                  param: Contract.Parameter,
                  context: Context): P[F, Either[Throwable, Unit]]
}
