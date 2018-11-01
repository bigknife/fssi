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
  import model.logService._
  import model.applicationService._

  /** send message to let peer nodes know
    * first, let the envelope be processed locally
    * if processed, then broadcast to scp network.
    */
  def emit(nodeId: NodeID,
           slotIndex: SlotIndex,
           previousValue: Value,
           message: Message): SP[F, Unit] =
    for {
      _        <- debug(s"[$nodeId][$slotIndex] try to emit message: $message")
      envelope <- putInEnvelope(nodeId, slotIndex, message)
      emitable <- canEmit(nodeId, slotIndex, envelope)
      _ <- ifThen(emitable) {
        for {
          _ <- info(s"[$nodeId][$slotIndex] can emit now")
          _ <- ifThen(handleSCPEnvelope(envelope, previousValue)) {
            for {
              _ <- info(s"[$nodeId][$slotIndex] message handled locally success")
              _ <- broadcastEnvelope(nodeId, slotIndex, envelope)
              _ <- info(s"[$nodeId][$slotIndex] broadcast message to peers")
            } yield ()
          }
        } yield ()
      }
    } yield ()
}
