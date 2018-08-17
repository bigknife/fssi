package fssi
package corenode

import bigknife.scalap.world.Connect
import bigknife.scalap.ast.types._

trait ConsensusConnect extends Connect {

  /**
    * try to extract a valid value from a not full validated value
    * @param value value not full validated
    * @return if possible, Some extracted value, or None
    */
  def extractValidValue(value: Value): Option[Value] = ???

  /**
    * application level validation
    * @param value value
    * @return full/ maybe/ invalid
    */
  def validateValue(value: Value): Value.Validity = ???

  /**
    * make a signature for data of a node
    * @param bytes data
    * @param nodeID node id
    * @return
    */
  def signData(bytes: Array[Byte], nodeID: NodeID): Signature = ???

  /**
    * broadcast message
    * @param envelope envelope
    */
  def broadcastMessage[M <: Message](envelope: Envelope[M]): Unit = ???

  /**
    * broad node quorum set
    * @param nodeID node
    * @param quorumSet quorum set
    */
  def synchronizeQuorumSet(nodeID: NodeID, quorumSet: QuorumSet): Unit = ???

  /**
    * verify signature of an envelope
    * @param envelope envelope
    * @return
    */
  def verifySignature[M <: Message](envelope: Envelope[M]): Boolean = ???

  /**
    * combine value set to one value
    * @param valueSet values
    * @return
    */
  def combineValues(valueSet: ValueSet): Value = ???

  /**
    * run abandon ballot with counter outside
    * @param counter ballot's counter
    */
  def runAbandonBallot(nodeID: NodeID, slotIndex: SlotIndex, counter: Int): Unit = ???

  /**
    * triggered when value externalized
    * @param nodeID node id
    * @param slotIndex slotIndex
    * @param value value
    */
  def valueExternalized(nodeID: NodeID, slotIndex: SlotIndex, value: Value): Unit = ???

  /**
    * timeout for next round
    * @param currentRound current round
    * @return timeout milliseconds
    */
  def timeoutForNextRoundNominating(currentRound: Int): Long = ???

  /**
    * trigger next round nominating
    * @param nodeID node id
    * @param slotIndex slotIndex
    * @param nextRound next round number
    * @param valueToNominate value to nominate
    * @param previousValue previous value
    * @param afterMilliSeconds after millis seconds
    * @return
    */
  def triggerNextRoundNominating(nodeID: NodeID,
                                 slotIndex: SlotIndex,
                                 nextRound: Int,
                                 valueToNominate: Value,
                                 previousValue: Value,
                                 afterMilliSeconds: Long): Unit = ???
}
