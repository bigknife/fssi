package fssi.consensus.scp.ast.usecase

import fssi.consensus.scp.ast.domain.types._
import bigknife.sop._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.components.Model

trait SCP[F[_]]
    extends SCPUseCases[F]
    with NominationProtocol[F]
    with BallotProtocol[F] {

  val model: Model[F]
  import model._

  /**
    * called when node accept an envelope of message.
    *
    * @param envelope message envelope
    * @return handling state
    */
  override def handleEnvelope(envelope: Envelope): SP[F, Envelope.State] = {
    // found or create a slot
    def getOrCreateSlot(index: Long): SP[F, Slot] =
      for {
        slotOpt <- slotStore.findSlot(index)
        slot <- if (slotOpt.isDefined) slotOpt.get.pureSP[F]
        else
          for {
            s0 <- slotService.createSlot(index)
            _ <- slotStore.saveSlot(s0)
          } yield s0
      } yield slot

    // if the statement in the envelope is NominateStatement, then nominate
    //  or do a ballot, that's to say, run corresponded protocol
    envelope.statement match {
      case x: Statement.NominationStatement =>
        for {
          slot <- getOrCreateSlot(envelope.statement.slotIndex)
          result <- runNominationProtocol(slot, envelope)
          _ <- slotStore.saveSlot(result._1)
        } yield result._2
      case x: Statement.BallotStatement =>
        for {
          slot <- getOrCreateSlot(envelope.statement.slotIndex)
          state <- runBallotProtocol(slot, envelope)
        } yield state
    }
  }
}
