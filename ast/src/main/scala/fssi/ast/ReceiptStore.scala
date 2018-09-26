package fssi
package ast

import types.biz._
import types.base._
import types.exception._

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import java.io._

@sp trait ReceiptStore[F[_]] {
  /** init data store
    */
  def initializeReceiptStore(receiptStoreRoot: File): P[F, Unit]
}
