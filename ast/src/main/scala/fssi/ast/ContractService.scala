package fssi
package ast

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

  /** create a user-contract from a compiled contract file
    */
  def createUserContractFromContractFile(contractFile: File): P[F, Either[FSSIException, Contract.UserContract]]
}
