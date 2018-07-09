package fssi.world.handler

import bigknife.scalap.ast.types._
import bigknife.scalap.world.Connect
import fssi.ast.domain.components.Model
import fssi.ast.domain.types.BytesValue
import fssi.ast.usecase.Warrior
import fssi.interpreter.{Setting, runner}

class SCPConnectHandler(setting: Setting, warrior: Warrior[Model.Op]) extends Connect {
  override def extractValidValue(value: Value): Option[Value] = None

  override def validateValue(value: Value): Value.Validity = {
    //todo validate more
    Value.Validity.FullyValidated
  }

  override def signData(bytes: Array[Byte], nodeID: NodeID): Signature = {
    val data =
      runner.runIO(warrior.signData(bytes, BytesValue(nodeID.bytes)), setting).unsafeRunSync()
    Signature(data.bytes)
  }

  override def broadcastMessage[M <: Message](envelope: Envelope[M]): Unit = {
    //todo message to json
    ???
  }

  override def verifySignature[M <: Message](envelope: Envelope[M]): Boolean = ???

  override def combineValues(valueSet: ValueSet): Value = ???

  override def runAbandonBallot(nodeID: NodeID, slotIndex: SlotIndex, counter: Int): Unit = ???

  override def valueExternalized(nodeID: NodeID, slotIndex: SlotIndex, value: Value): Unit = ???
}
