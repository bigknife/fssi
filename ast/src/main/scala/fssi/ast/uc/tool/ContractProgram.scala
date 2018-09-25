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
  def compileContract(projectRoot: File, output: File, sandboxVersion: String): SP[F, Unit] = {
    /*
        import contractService._

    for {
      compileEither  <- compileContractProject(projectDirectory, sandboxVersion, output)
      _              <- err.either(compileEither)
      determinEither <- checkDeterminismOfContractProject(output)
      _              <- err.either(determinEither)
    } yield ()
     */
    ???
  }
}
