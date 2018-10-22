package fssi.scp
package ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import scala.collection.immutable._

import types._


@sp trait ApplicationService[F[_]] {

  /** validate value on application level
    */
  def validateValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Value.Validity]

  /** validate some values
    */
  def validateValues(nodeId: NodeID, slotIndex: SlotIndex, values: ValueSet): P[F, Value.Validity]

  /** combine values to ONE value, maybe nothing
    */
  def combineCandidates(nodeId: NodeID, slotIndex: SlotIndex, xs: ValueSet): P[F, Option[Value]]

  /** extract valida value from a not fully validated value
    */
  def extractValidValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Option[Value]]

  

  /** after timeout milliseconds, execute the program
    * @param tag the delay timer tag, we can cancel the timer by using this tag later.
    * @param program with type: SP[F, Unit]
    */
  def delayExecuteProgram(tag: String, program: Any, timeout: Long): P[F, Unit]

  /** cancel the timer
    */
  def stopDelayTimer(tag: String): P[F, Unit]
}
