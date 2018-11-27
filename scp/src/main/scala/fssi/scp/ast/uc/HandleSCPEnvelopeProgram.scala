package fssi.scp
package ast
package uc

import types._
import components._
import bigknife.sop._
import bigknife.sop.implicits._
import cats.free.FreeApplicative.FA

trait HandleSCPEnvelopeProgram[F[_]] extends SCP[F] with BaseProgram[F] {
  import model.nodeService._
  import model.nodeStore._
  import model.logService._

  /** process message envelope from peer nodes
    */
  override def handleSCPEnvelope[M <: Message](envelope: Envelope[M],
                                               previousValue: Value): SP[F, Boolean] = {

    val statement = envelope.statement
    val nodeId    = statement.from
    val slotIndex = statement.slotIndex
    val message   = statement.message

    def checkEnvelope: SP[F, Boolean] =
      ifM(isOlderEnvelope(nodeId, slotIndex, envelope), false.pureSP[F])(
        ifM(isSignatureTampered(envelope), false.pureSP[F])(
          isStatementValid(nodeId, slotIndex, statement)))
    def envelopeCheckingFailed = checkEnvelope.map(!_)

    ifM(envelopeCheckingFailed, false.pureSP[F]) {
      for {
//        _ <- info(s"[$nodeId][$slotIndex] handling scp envelope with message: $message)")
        _ <- saveEnvelope(nodeId, slotIndex, envelope)
        _ <- cacheNodeQuorumSet(nodeId, envelope.statement.quorumSet)
        //_ <- debug(s"[$nodeId][$slotIndex] saved sane scp envelope")
        _ <- infoReceivedEnvelope(envelope)
        handled <- message match {
          case _: Message.Nomination =>
            for {
              _ <- debug(s"[$nodeId][$slotIndex] handling nomination envelope")
              x <- handleNomination(nodeId,
                                    slotIndex,
                                    previousValue,
                                    statement.asInstanceOf[Statement[Message.Nomination]])
              _ <- debug(s"[$nodeId][$slotIndex] handled nomination envelope: $x")
            } yield x

          case _: Message.BallotMessage =>
            for {
              _ <- debug(s"[$nodeId][$slotIndex] handling ballot envelope")
//              x <- handleBallotMessage(nodeId,
//                                       slotIndex,
//                                       previousValue,
//                                       envelope.asInstanceOf[Envelope[Message.BallotMessage]])
//              _ <- debug(s"[$nodeId][$slotIndex] handled ballot envelope: $x")
              _ <- debug(s"[$nodeId][$slotIndex] handled ballot envelope: ")
            } yield true
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
