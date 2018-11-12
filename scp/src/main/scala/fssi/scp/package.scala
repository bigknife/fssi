package fssi
import fssi.scp.ast.uc.SCP
import fssi.scp.interpreter.Setting
import fssi.scp.types._

package object scp {
  type ApplicationCallback = interpreter.ApplicationCallback
  val ApplicationCallback: interpreter.ApplicationCallback.type = interpreter.ApplicationCallback

  object Portal {

    def initialize(implicit setting: Setting): Unit = {
      val scp     = SCP[fssi.scp.ast.components.Model.Op]
      val program = scp.initialize()
      fssi.scp.interpreter.runner.runIO(program, setting).unsafeRunSync()
    }

    def handleRequest(nodeId: NodeID, slotIndex: SlotIndex, previousValue: Value, value: Value)(
        implicit setting: Setting): Boolean = {
      val scp     = SCP[fssi.scp.ast.components.Model.Op]
      val program = scp.handleAppRequest(nodeId, slotIndex, value, previousValue)
      fssi.scp.interpreter.runner.runIO(program, setting).unsafeRunSync()
    }

    def handleEnvelope[M <: Message](nodeId: NodeID,
                                     slotIndex: SlotIndex,
                                     envelope: Envelope[M],
                                     previousValue: Value)(implicit setting: Setting): Boolean = {
      val scp     = SCP[fssi.scp.ast.components.Model.Op]
      val program = scp.handleSCPEnvelope(nodeId, slotIndex, envelope, previousValue)
      fssi.scp.interpreter.runner.runIO(program, setting).unsafeRunSync()
    }
  }
}
