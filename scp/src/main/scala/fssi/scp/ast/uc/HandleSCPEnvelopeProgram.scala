package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleSCPEnvelopeProgram[F[_]] extends SCP[F] with BaseProgram[F] {
  import model.nodeService._
  import model.nodeStore._

  
  /** process message envelope from peer nodes
    */
  override def handleSCPEnvelope[M <: Message](nodeId: NodeID,
                                      slotIndex: SlotIndex,
                                      envelope: Envelope[M],
                                      previousValue: Value): SP[F, Boolean] = {

    val statement = envelope.statement
    val message   = statement.message

    def checkEnvelope: SP[F, Boolean] =
      ifM(isOlderEnvelope(nodeId, slotIndex, envelope), false.pureSP[F])(
        ifM(isSignatureTampered(envelope), false.pureSP[F])(
          ifM(isStatementInvalid(statement), false.pureSP[F])(isMessageSane(message))))
    def envelopeCheckingFailed = checkEnvelope.map(!_)

    ifM(envelopeCheckingFailed, false.pureSP[F]) {
      for {
        _ <- saveEnvelope(nodeId, slotIndex, envelope)
        handled <- message match {
          case _: Message.Nomination =>
            handleNomination(nodeId,
                             slotIndex,
                             previousValue,
                             statement.asInstanceOf[Statement[Message.Nomination]])
          case _: Message.BallotMessage =>
            handleBallotMessage(nodeId,
                                slotIndex,
                                previousValue,
                                envelope.asInstanceOf[Envelope[Message.BallotMessage]])
        }
      } yield handled

    }
  }

  def handleNomination(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       previousValue: Value,
                       statement: Statement[Message.Nomination]): SP[F, Boolean]

  def handleBallotMessage(nodeId: NodeID,
                          slotIndex: SlotIndex,
                          previousValue: Value,
                          envelope: Envelope[Message.BallotMessage]): SP[F, Boolean]
}
