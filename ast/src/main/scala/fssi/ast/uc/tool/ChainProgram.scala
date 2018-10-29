package fssi.ast.uc
package tool

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.base._
import fssi.types.biz._
import java.io._

trait ChainProgram[F[_]] extends ToolProgram[F] with BaseProgram[F] {
  import model._

  /** Create a chain
    * @param dataDir directory where the chain data saved
    * @param chainId the chain id
    */
  def createChain(rootDir: File, chainId: String): SP[F, Unit] = ???
}
