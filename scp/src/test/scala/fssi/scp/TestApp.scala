package fssi.scp
import java.util.concurrent.{ExecutorService, Executors}

import fssi.scp.ast.components.Model
import fssi.scp.ast.components.Model.Op
import fssi.scp.ast.uc.SCP
import fssi.scp.interpreter.{Setting, runner}
import fssi.scp.types.Message.Nomination
import fssi.scp.types._
import fssi.utils.cryptoUtil

class TestApp(nodeID: NodeID, slotIndex: SlotIndex, quorumSet: QuorumSet)
    extends interpreter.ApplicationCallback {

  // all statements sent to network
  private var statements: Vector[Statement[_ <: Message]] = Vector.empty

  // expected candidates and predefined composite values, should be set before a nomination step
  private var expectedCandidates: ValueSet          = ValueSet.empty
  private var expectedCompositeValue: Option[Value] = None

  private var nodeWithTopPriority: Option[NodeID] = None

  private val started: Timestamp = Timestamp(0l)

  private val service = Executors.newSingleThreadExecutor()

  private val setting: Setting = Setting(
    quorumSet = quorumSet,
    privateKey = cryptoUtil.generateECKeyPair(cryptoUtil.SECP256K1).getPrivate,
    applicationCallback = this
  )
  private val scp: SCP[Op] = SCP[Model.Op]

  def reset(): Unit = {
    statements = Vector.empty

    expectedCandidates = ValueSet.empty
    expectedCompositeValue = None
  }

  override def validateValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Value.Validity = {
    Value.Validity.FullyValidated
  }

  override def combineValues(nodeId: NodeID,
                             slotIndex: SlotIndex,
                             value: ValueSet): Option[Value] = {
    require(value == expectedCandidates)

    expectedCompositeValue
  }

  override def extractValidValue(nodeId: NodeID,
                                 slotIndex: SlotIndex,
                                 value: Value): Option[Value] = {
    None
  }

  override def scpExecutorService(): ExecutorService = {
    service
  }

  override def valueConfirmed(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit = {}

  override def valueExternalized(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit = {}

  override def broadcastEnvelope[M <: Message](nodeId: NodeID,
                                               slotIndex: SlotIndex,
                                               envelope: Envelope[M]): Unit = {
    if (isEmittedFromThisNode(envelope)) statements = statements :+ envelope.statement
  }

  override def isHashFuncProvided: StateChanged = true

  override def hashNodeForPriority(nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   previousValue: Value,
                                   round: Int): Long =
    if (nodeWithTopPriority exists (_ === nodeId)) 1000l else 0l

  override def hashNodeForNeighbour(nodeId: NodeID,
                                    slotIndex: SlotIndex,
                                    previousValue: Value,
                                    round: Int): Long =
    if (nodeWithTopPriority exists (_ === nodeId)) 1000l else 0l

  def liftNodePriority(nodeID: NodeID): Unit = {
    nodeWithTopPriority = Some(nodeID)
  }

  def forecastNomination(candidates: ValueSet, compositeValue: Option[Value]): Unit = {
    expectedCandidates = candidates
    expectedCompositeValue = compositeValue
  }

  def hasNominated(voted: ValueSet, accepted: ValueSet): Boolean =
    statements.lastOption map (_.copy(timestamp = started)) contains statementOf(
      Nomination(voted, accepted))

  def nominate(value: Value): Boolean = {
    val p = scp.handleAppRequest(nodeID, slotIndex, value, value)
    runner.runIO(p, setting).unsafeRunSync
  }

  private def isEmittedFromThisNode[M <: Message](envelope: Envelope[M]): Boolean =
    envelope.statement.from == nodeID &&
      envelope.statement.slotIndex == slotIndex && envelope.statement.quorumSet == quorumSet

  private def statementOf[M <: Message](message: M): Statement[M] = Statement[M](
    nodeID,
    slotIndex,
    started,
    quorumSet,
    message
  )
}
