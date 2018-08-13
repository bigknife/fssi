package fssi
package ast

import types._, exception._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import java.io._

@sp trait ChainStore[F[_]] {

  /** initialize a data directory to be a token store
    * @param dataDir directory to save token.
    */
  def createChainRoot(dataDir: File, chainID: String): P[F, Either[FSSIException, File]]
}
