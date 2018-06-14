package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types._

@sp trait SlotService[F[_]] {
  /**
    * create a new slot
    * @param index slot index
    * @return
    */
  def createSlot(index: Long): P[F, Slot]

  /**
    * compute two slot, to resolve the change happened from s1 to s2
    * @param s1 slot 1
    * @param s2 slot 2
    * @return
    */
  def resolveChange(s1: Slot, s2: Slot): P[F, Slot.Change]

  /**
    * validate a value for nomination
    * @param slot current slot
    * @param value value
    * @return
    */
  def validateValueForNomination(slot: Slot, value: Value): P[F, Value.ValidationLevel]

  /**
    * validate a value for Ballot
    * @param slot current slot
    * @param value value
    * @return
    */
  def validateValueForBallot(slot: Slot, value: Value): P[F, Value.ValidationLevel]

  /**
    * extract valid value
    * @param value value
    * @return
    */
  def extractValidValue(value: Value): P[F, Option[Value]]

  /**
    * nominating a new vote, change the slot state
    * @param slot slot
    * @param newVote new value to vote
    * @return
    */
  def nominatingValue(slot: Slot, newVote: Value): P[F, Slot]

  /**
    * compute value's hash
    * @param slot slot
    * @param value value
    * @return
    */
  def hashValue(slot: Slot, value: Value): P[F, Long]

  /**
    * if slot's node is a validator, emit envelop
    * @param slot slot
    * @param envelope envelope
    * @return
    */
  def emitEnvelope(slot: Slot, envelope: Envelope): P[F, Unit]

  /**
    * combine candidates to a value
    * @param slot slot
    * @return
    */
  def combineCandidates(slot: Slot): P[F, Value]

  /**
    * bump state ???
    * @param slot slot
    * @param value value
    * @param force force
    * @return
    */
  def bumpState(slot: Slot, value: Value, force: Boolean): P[F, Slot]

  /**
    * set values of slot may be valid
    * @param slot slot
    * @return
    */
  def setMaybeValid(slot: Slot): P[F, Unit]
}
