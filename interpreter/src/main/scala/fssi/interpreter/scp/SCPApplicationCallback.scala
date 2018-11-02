package fssi.interpreter.scp
import java.util.concurrent.{ExecutorService, Executors}

import fssi.scp.interpreter.ApplicationCallback
import fssi.scp.types._

class SCPApplicationCallback extends ApplicationCallback {

  override def validateValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Value.Validity =
    ???

  override def combineValues(nodeId: NodeID, slotIndex: SlotIndex, value: ValueSet): Option[Value] =
    ???

  override def extractValidValue(nodeId: NodeID,
                                 slotIndex: SlotIndex,
                                 value: Value): Option[Value] = ???

  override def scpExecutorService(): ExecutorService = Executors.newCachedThreadPool()

  override def valueConfirmed(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit = ???

  override def valueExternalized(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit = ???

  override def broadcastEnvelope[M <: Message](nodeId: NodeID,
                                               slotIndex: SlotIndex,
                                               envelope: Envelope[M]): Unit = ???
}
