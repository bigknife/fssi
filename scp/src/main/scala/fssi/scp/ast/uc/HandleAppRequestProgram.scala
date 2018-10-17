package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleRequestProgram[F[_]] extends BaseProgram[F] {
  /** handle request of application
    */
  def handleAppRequest(nodeId: NodeID, slotIndex: SlotIndex, value: Value): SP[F, Boolean] = {
    ???
  }
}
