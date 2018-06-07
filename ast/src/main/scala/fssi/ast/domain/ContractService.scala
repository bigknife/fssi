package fssi.ast.domain

import java.nio.file.Path

import bigknife.sop._
import implicits._
import macros._
import fssi.ast.domain.exceptions._
import fssi.ast.domain.types._
import fssi.contract.States

@sp trait ContractService[F[_]] {

  /**
    * create a contract, but the signature is Empty
    * @param name name
    * @param version version
    * @param code code (base64 encoded jar)
    * @return
    */
  def createContractWithoutSing(name: String,
                                version: String,
                                code: String): P[F, Contract.UserContract]

  /** resolve a transaction to contract */
  def resolveTransaction(transaction: Transaction): P[F, (Contract.Name, Contract.Version, Option[Contract.Parameter])]

  /** run the contract */
  def runContract(invoker: Account,
                  contract: Contract,
                  currentStates: States,
                  parameter: Option[Contract.Parameter],
                  transactionId: Transaction.ID): P[F, Either[Throwable, Moment]]

  /** compile source code to class file */
  def compileContractSourceCode(source: Path): P[F, Either[ContractCompileError, Path]]

  /** check the contract classes are deterministic */
  def checkDeterministicOfClass(classFilePath: Path): P[F, Either[ContractCompileError, Path]]

  /** package all contract classes to jar */
  def jarContract(classFilePath: Path): P[F, BytesValue]

  def createParameterFromString(
      params: String): P[F, Either[IllegalContractParams, Contract.Parameter]]
}
