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
  def createContractProject(root: File): SP[F, Unit] = ???

  /** compile contract project
    * @param projectRoot the root path of the contract project
    * @param output the contract target file
    * @param sandboxVersion the adapted version for contract
    */
  def compileContract(accountFile: File,
                      secretKeyFile: File,
                      projectRoot: File,
                      output: File,
                      sandboxVersion: String): SP[F, Unit] = ???
}
