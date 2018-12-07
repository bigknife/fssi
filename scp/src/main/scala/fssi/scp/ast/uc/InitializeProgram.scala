package fssi
package scp
package ast
package uc
import bigknife.sop._
import bigknife.sop.implicits._
import fssi.scp.types.{NodeID, QuorumSet, SlotIndex}
import types._

trait InitializeProgram[F[_]] extends SCP[F] with BaseProgram[F] {

  import model._

  def initialize(nodeId: NodeID, quorumSet: QuorumSet, currentSlotIndex: SlotIndex): SP[F, Unit] = {
    for {
      _ <- nodeService.cacheNodeQuorumSet(nodeId, quorumSet)
      _ <- nominateFakeValue(currentSlotIndex + 1)
      _ <- broadcastMessageRegularly()
    } yield ()
  }

  def nominateFakeValue(slotIndex: SlotIndex): SP[F, Unit] = {
    for {
      fakeValue <- nodeService.blockFakeValue(slotIndex)
      nodeId    <- nodeStore.localNode()
      _ <- applicationService.delayExecuteProgram(
        NOMINATE_TIMER,
        handleAppRequest(nodeId, slotIndex, fakeValue, fakeValue),
        0)
    } yield ()
  }
}
