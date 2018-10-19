package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait EmitProgram[F[_]] extends SCP[F] with BaseProgram[F] {
  import model.nodeService._
  import model.nodeStore._

  /** send message to let peer nodes know
    * first, let the envelope be processed locally
    * if processed, then broadcast to scp network.
    */
  def emit(nodeId: NodeID,
           slotIndex: SlotIndex,
           previousValue: Value,
           message: Message): SP[F, Unit] =
    for {
      envelope <- putInEnvelope(nodeId, message)
      _ <- ifThen(canEmit(nodeId, slotIndex, envelope)) {
        ifThen(handleSCPEnvelope(nodeId, slotIndex, envelope, previousValue)) {
          broadcastEnvelope(nodeId, envelope)
        }
      }

    } yield ()

}
