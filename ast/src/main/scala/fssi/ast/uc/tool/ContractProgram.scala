package fssi
package ast
package uc
package tool

import types._
import utils._
import types.syntax._
import bigknife.sop._
import bigknife.sop.implicits._

import java.io._

trait ContractProgram[F[_]] extends BaseProgram[F] {
  import model._

  /** create a contract project
    */
  def createContractProject(projectRoot: File): SP[F, Unit] = {
    ???
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
    import contractService._
    import accountStore._
    import accountService._
    for {
      account   <- loadAccountFromFile(accountFile).right
      secretKey <- loadAccountSecretKeyFile(secretKeyFile).right
      privKey   <- aesDecryptPrivKey(account.encPrivKey, secretKey, account.iv).right
      _ <- compileContractProject(account.id,
                                  account.pubKey,
                                  privKey,
                                  projectRoot,
                                  sandboxVersion,
                                  output).right
      _ <- checkDeterminismOfContractProject(output).right
    } yield ()
  }
}
