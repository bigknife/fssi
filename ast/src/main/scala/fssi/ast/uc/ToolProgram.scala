package fssi.ast
package uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.base._
import fssi.types.biz._
import fssi.ast.uc.tool._
import java.io._

trait ToolProgram[F[_]] {

  /** create an account that is compatible with btc
    */
  def createAccount(seed: RandomSeed): SP[F, (Account, Account.SecretKey)]

  /** create a contract project
    */
  def createContractProject(projectRoot: File): SP[F, Unit]

  /** compile contract project
    * @param projectRoot the root path of the contract project
    * @param output the contract target file
    * @param sandboxVersion the adapted version for contract
    */
  def compileContract(accountFile: File,
                      secretKeyFile: File,
                      projectRoot: File,
                      output: File,
                      sandboxVersion: String): SP[F, Unit]

  /** Create a chain
    * @param dataDir directory where the chain data saved
    * @param chainId the chain id
    */
  def createChain(rootDir: File, chainId: String): SP[F, Unit]

  /** create transfer transaction
    */
  def createTransferTransaction(accountFile: File,
                                secretKeyFile: File,
                                payee: Account.ID,
                                token: Token): SP[F, Transaction.Transfer]

  /** create a deploy transaction
    */
  def createDeployTransaction(accountFile: File,
                              secretKeyFile: File,
                              contractFile: File): SP[F, Transaction.Deploy]

  /** create a run(user contract) transaction
    */
  def createRunTransaction(
      accountFile: File,
      secretKeyFile: File,
      contractName: UniqueName,
      contractVersion: Contract.Version,
      methodAlias: String,
      parameter: Option[Contract.UserContract.Parameter]): SP[F, Transaction.Run]
}

object ToolProgram {
  def apply[F[_]](implicit M: blockchain.Model[F]): ToolProgram[F] =
    new AccountProgram[F] with ContractProgram[F] with ChainProgram[F] with TransactionProgram[F] {
      private[uc] val model: blockchain.Model[F] = M
    }
}
