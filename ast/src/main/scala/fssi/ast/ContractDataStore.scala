package fssi
package ast

import types._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import java.io._

@sp trait ContractDataStore[F[_]] {

  /** initialize a data directory to be a contract data store
    * @param dataDir directory to save contract data.
    */
  def initialize(dataDir: File): P[F, Unit]
}
