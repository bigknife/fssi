package fssi.ast.domain

import java.nio.file.Path

import bigknife.sop._, implicits._, macros._
import fssi.ast.domain.exceptions.ContractCompileError
import fssi.ast.domain.types._

@sp trait ContractService[F[_]] {
  /** resolve a transaction to contract */
  def resolveTransaction(transaction: Transaction): P[F, (Contract.Name, Contract.Version)]

  /** run the contract */
  def runContract(invoker: Account, contract: Contract): P[F, Either[Throwable, Moment]]

  /** compile source code to class file */
  def compileContractSourceCode(source: Path): P[F, Either[ContractCompileError,Path]]

  /** check the contract classes are deterministic */
  def checkDeterministicOfClass(classFilePath: Path): P[F, Either[ContractCompileError, Path]]

  /** package all contract classes to jar */
  def jarContract(classFilePath: Path): P[F, BytesValue]
}
