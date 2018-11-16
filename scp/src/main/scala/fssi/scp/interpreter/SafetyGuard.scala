package fssi.scp
package interpreter

import types._
import store._

trait SafetyGuard {
  private val highestSlotIndex: Var[Map[NodeID, SlotIndex]] = Var(Map.empty)

  def assertSlotIndex(nodeId: NodeID, slotIndex: SlotIndex): Unit = {
    highestSlotIndex.map(_.get(nodeId)).foreach {
      case Some(oldSlotIndex) =>
        if (slotIndex.value > oldSlotIndex.value)
          highestSlotIndex := highestSlotIndex.map(_ + (nodeId -> slotIndex)).unsafe
        else if (slotIndex.value < oldSlotIndex.value)
          throw new RuntimeException(
            s"invalid slotIndex: $slotIndex, because a higer one exists now")
        else ()
      case None =>
        highestSlotIndex := highestSlotIndex.map(_ + (nodeId -> slotIndex)).unsafe
    }
  }

  private[scp] def resetSlotIndex(nodeID: NodeID): Unit = {
    highestSlotIndex := highestSlotIndex.map(_.filterKeys(_ != nodeID)).unsafe()
  }
}
