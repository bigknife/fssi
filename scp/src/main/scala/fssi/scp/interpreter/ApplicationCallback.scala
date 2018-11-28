package fssi.scp.interpreter

import fssi.scp.types._

/** SCP application callback
  *
  */
trait ApplicationCallback {
  def validateValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Value.Validity
  def combineValues(nodeId: NodeID, slotIndex: SlotIndex, value: ValueSet): Option[Value]
  def extractValidValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Option[Value]
  def valueConfirmed(slotIndex: SlotIndex, value: Value): Unit
  def valueExternalized(slotIndex: SlotIndex, value: Value): Unit
  def broadcastEnvelope[M <: Message](slotIndex: SlotIndex, envelope: Envelope[M]): Unit
  def ballotDidHearFromQuorum(slotIndex: SlotIndex, ballot: Ballot): Unit = ()

  def isValidator: Boolean = true

  def isHashFuncProvided: Boolean = false
  def hashNodeForPriority(nodeId: NodeID,
                          slotIndex: SlotIndex,
                          previousValue: Value,
                          round: Int): Long = 0
  def hashNodeForNeighbour(nodeId: NodeID,
                           slotIndex: SlotIndex,
                           previousValue: Value,
                           round: Int): Long                                                = 0
  def hashValue(slotIndex: SlotIndex, previousValue: Value, round: Int, value: Value): Long = 0

  def canDispatch: Boolean                                         = false
  def dispatch(timer: String, timeout: Long, task: Runnable): Unit = ()
  def cancel(timer: String): Unit                                  = ()
  def currentSlotIndex(): SlotIndex
}

object ApplicationCallback {
  private def warning(methodName: String): Exception =
    new UnsupportedOperationException(
      s"unimplemented method:$methodName PLEASE IMPLEMENT APPLICATION_CALLBACK")

  val unimplemented: ApplicationCallback = new ApplicationCallback {
    override def validateValue(nodeId: NodeID,
                               slotIndex: SlotIndex,
                               value: Value): Value.Validity = {
      throw warning("validateValue")
    }

    override def combineValues(nodeId: NodeID,
                               slotIndex: SlotIndex,
                               value: ValueSet): Option[Value] = {
      throw warning("combineValues")
    }

    override def extractValidValue(nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   value: Value): Option[Value] = {
      throw warning("extractValidValue")
    }

    override def dispatch(timer: String, timeout: Long, task: Runnable): Unit = {
      throw warning("dispatch")
    }

    override def cancel(timer: String): Unit = {
      throw warning("cancel")
    }

    override def valueConfirmed(slotIndex: SlotIndex, value: Value): Unit = {
      throw warning("valueConfirmed")
    }

    override def valueExternalized(slotIndex: SlotIndex, value: Value): Unit = {
      throw warning("valueExternalized")
    }

    override def broadcastEnvelope[M <: Message](slotIndex: SlotIndex,
                                                 envelope: Envelope[M]): Unit = {
      throw warning("broadcastEnvelope")
    }

    override def ballotDidHearFromQuorum(slotIndex: SlotIndex, ballot: Ballot): Unit = {
      throw warning("ballotDidHearFromQuorum")
    }
    override def currentSlotIndex(): SlotIndex = ???
  }
}
