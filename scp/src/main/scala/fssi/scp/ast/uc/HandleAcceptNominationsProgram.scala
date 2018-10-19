package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleAcceptNominationsProgram[F[_]] extends HandleVoteNominationsProgram[F] {
  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._

  def handleAcceptNominations(nodeId: NodeID,
                              slotIndex: SlotIndex,
                              previousValue: Value,
                              statement: Statement[Message.AcceptNominations]): SP[F, Boolean] = {

    // remove values which have been voted or accepted
    lazy val completelyNewValues: SP[F, ValueSet] =
      statement.message.values.foldLeft(ValueSet.empty.pureSP[F]) { (acc, n) =>
        for {
          pre <- acc
          a   <- valueAccepted(nodeId, slotIndex, n)
        } yield if (a) pre else pre + n
      }

    // because a vote(nominate x) should be received before, it's not necessary
    // to try to set x which accepted in the statement to local votes once more.
    // So, we can do a federated voting to (accept (nominate x)), and then do a
    // federated ratifying to (confirm (nominate x)) to produce a candidate value.
    // Once we get a candidate value, try to do ballot stuff.

    def tryAccept(xs: ValueSet): SP[F, ValueSet] =
      xs.foldLeft(ValueSet.empty.pureSP[F]) { (acc, n) =>
        for {
          pre           <- acc
          acceptedNodes <- nodesAcceptedNomination(nodeId, slotIndex, n)
          accepted <- ifM(isVBlocking(nodeId, acceptedNodes), right = true) {
            for {
              votedNodes <- nodesVotedNomination(nodeId, slotIndex, n)
              byQuorum   <- isQuorum(nodeId, acceptedNodes ++ votedNodes)
            } yield byQuorum
          }
          //_ <- ifThen(accepted)(acceptNewNominations(nodeId, slotIndex, ValueSet(n)))
        } yield if (accepted) pre + n else pre
      }

    // after trying accept, local status (of accepted votes) has been updated
    // now, try to combine a candidate value from local accepted votes
    def tryCandidate(xs: ValueSet): SP[F, ValueSet] =
      xs.foldLeft(ValueSet.empty.pureSP[F]) { (acc, n) =>
        for {
          pre              <- acc
          acceptedNodes    <- nodesAcceptedNomination(nodeId, slotIndex, n)
          ratifiedAccepted <- isQuorum(nodeId, acceptedNodes)
        } yield if (ratifiedAccepted) pre + n else pre
      }

    // send message to let peer nodes know
    def emit(message: Option[Message]): SP[F, Unit] =
      if (message.isDefined)
        for {
          envelope <- putInEnvelope(nodeId, message.get)
          _        <- handleSCPEnvelope(nodeId, slotIndex, envelope, previousValue)
          _        <- broadcastEnvelope(nodeId, envelope)
        } yield ()
      else ()

    for {
      _ <- handleVoteNominations(nodeId, slotIndex, previousValue, statement.withMessage(statement.message.asVote))
      newAccepted     <- completelyNewValues
      accepted        <- tryAccept(newAccepted)
      _               <- ifThen(accepted.nonEmpty)(acceptNewNominations(nodeId, slotIndex, accepted))
      currentAccepted <- acceptedNominations(nodeId, slotIndex)
      candidates      <- tryCandidate(currentAccepted)
      candidateValue  <- combineCandidates(candidates)
      _ <- ifThen(candidateValue.isDefined)(
        nominateAsCandidate(nodeId, slotIndex, candidateValue.get))
      acceptMessage <- ifM(accepted.isEmpty && candidateValue.isEmpty,
                           Option.empty[Message.AcceptNominations]) {
        for {
          msg <- createAcceptNominationMessage(nodeId, slotIndex)
        } yield Option(msg)
      }
      _ <- emit(acceptMessage)

    } yield accepted

    ???
  }
}
