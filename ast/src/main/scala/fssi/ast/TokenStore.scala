package fssi
package ast

import types._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import java.io._

@sp trait TokenStore[F[_]] {

  /** initialize a data directory to be a token store
    * @param dataDir directory to save token.
    */
  def initialize(dataDir: File): P[F, Unit]
}
