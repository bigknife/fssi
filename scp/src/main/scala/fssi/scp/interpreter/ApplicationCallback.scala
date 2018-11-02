package fssi.scp.interpreter

import java.util.concurrent.ExecutorService

import fssi.scp.types._

/** SCP application callback
  *
  */
trait ApplicationCallback {
  def validateValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Value.Validity
  def combineValues(nodeId: NodeID, slotIndex: SlotIndex, value: ValueSet): Option[Value]
  def extractValidValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Option[Value]
  def dispatch(timer: String, task: Runnable): Unit
  def valueConfirmed(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit
  def valueExternalized(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit
  def broadcastEnvelope[M <: Message](nodeId: NodeID, slotIndex: SlotIndex, envelope: Envelope[M]): Unit

  def isHashFuncProvided: Boolean = false
  def hashNodeForPriority(nodeId: NodeID, slotIndex: SlotIndex, previousValue: Value, round: Int): Long = 0
  def hashNodeForNeighbour(nodeId: NodeID, slotIndex: SlotIndex, previousValue: Value, round: Int): Long = 0
}

object ApplicationCallback {
  private def warning(methodName: String): Exception = new UnsupportedOperationException(s"unimplemented method:$methodName PLEASE IMPLEMENT APPLICATION_CALLBACK")

  val unimplemented: ApplicationCallback = new ApplicationCallback {
    override def validateValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Value.Validity = {
      throw warning("validateValue")
    }

    override def combineValues(nodeId: NodeID, slotIndex: SlotIndex, value: ValueSet): Option[Value] = {
      throw warning("combineValues")
    }

    override def extractValidValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Option[Value] = {
      throw warning("extractValidValue")
    }

    override def dispatch(timer: String, task: Runnable): Unit= {
      throw warning("scpExecutorService")
    }

    override def valueConfirmed(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit = {
      throw warning("valueConfirmed")
    }

    override def valueExternalized(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit = {
      throw warning("valueExternalized")
    }

    override def broadcastEnvelope[M <: Message](nodeId: NodeID, slotIndex: SlotIndex, envelope: Envelope[M]): Unit = {
      throw warning("broadcastEnvelope")
    }
  }
}