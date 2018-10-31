package fssi.ast.uc
package tool

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.base._
import fssi.types.biz._
import java.io._

trait ContractProgram[F[_]] extends ToolProgram[F] with BaseProgram[F] {
  import model._

  /** create a contract project
    */
  def createContractProject(root: File): SP[F, Unit] = {
    for {
      createdOrFailed <- contract.createContractProject(root)
      _               <- err.either(createdOrFailed)
    } yield ()
  }

  /** compile contract project
    * @param projectRoot the root path of the contract project
    * @param output the contract target file
    * @param sandboxVersion the adapted version for contract
    */
  def compileContract(accountFile: File,
                      secretKeyFile: File,
                      projectRoot: File,
                      output: File,
                      sandboxVersion: String): SP[F, Unit] = {
    for {
      accountOrFailed   <- store.loadAccountFromFile(accountFile)
      account           <- err.either(accountOrFailed)
      secretKeyOrFailed <- store.loadSecretKeyFromFile(secretKeyFile)
      secretKey         <- err.either(secretKeyOrFailed)
      privateKey        <- crypto.decryptAccountPrivKey(account.encPrivKey, secretKey, account.iv)
      compileEither <- contract.compileContractProject(account.id,
                                                       account.pubKey,
                                                       privateKey,
                                                       projectRoot,
                                                       sandboxVersion,
                                                       output)
      _           <- err.either(compileEither)
      checkEither <- contract.checkContractDeterminism(account.pubKey, output)
      _           <- err.either(checkEither)
    } yield ()
  }
}
