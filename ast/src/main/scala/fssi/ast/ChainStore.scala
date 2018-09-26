package fssi
package ast

import types._, exception._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import java.io._

@sp trait ChainStore[F[_]] {

  /** create directory skeleton for a chain
    * if the root directory exists or is not empty, creation will fail.
    */
  def createChainSkeleton(rootDir: File, chainID: String): P[F, Either[FSSIException, File]]

  /** return block store path
    */
  def getBlockStoreRoot(chainRoot: File): P[F, File]

  /** return token store path
    */
  def getTokenStoreRoot(chainRoot: File): P[F, File]

  /** return contract store path
    */
  def getContractStoreRoot(chainRoot: File): P[F, File]

  /** return data store path
    */
  def getDataStoreRoot(chainRoot: File): P[F, File]

  /** return receipt store path
    */
  def getReceiptStoreRoot(chainRoot: File): P[F, File]

  /** create a default config file for core node
    */
  def createDefaultConfigFile(chainRoot: File): P[F, File]
}
