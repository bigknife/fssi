package fssi
package scp
package ast
package uc
import bigknife.sop._
import bigknife.sop.implicits._
import fssi.scp.types.{NodeID, QuorumSet, SlotIndex}

trait InitializeProgram[F[_]] extends SCP[F] with BaseProgram[F] {

  import model._

  def initialize(nodeId: NodeID, quorumSet: QuorumSet, slotIndex: SlotIndex): SP[F, Unit] = {
    for {
      _ <- nodeService.cacheNodeQuorumSet(nodeId, quorumSet)
      _ <- nominateFakeValue(nodeId, slotIndex)
//      _ <- broadcastMessageRegularly(slotIndex)
    } yield ()
  }

  def nominateFakeValue(nodeId: NodeID, slotIndex: SlotIndex): SP[F, Unit] = {
    for {
      fakeValue <- nodeService.blockFakeValue(slotIndex)
      _         <- handleAppRequest(nodeId, slotIndex, fakeValue, fakeValue)
    } yield ()
  }
}
