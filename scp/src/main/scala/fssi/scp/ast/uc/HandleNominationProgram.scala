package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleNominationProgram[F[_]] extends SCP[F] with EmitProgram[F] {

  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._

  def handleNomination(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       previousValue: Value,
                       statement: Statement[Message.Nomination]): SP[F, Boolean] = {

    ifM(isNominatingStopped(nodeId, slotIndex), false.pureSP[F]) {
      // try to accept votes from the message,
      def tryAcceptVotes(xs: ValueSet): SP[F, StateChanged] = {
        xs.foldLeft(false.pureSP[F]) { (acc, n) =>
          for {
            pre           <- acc
            acceptedNodes <- nodesAcceptedNomination(nodeId, slotIndex, n)
            changed <- ifM(isVBlocking(nodeId, acceptedNodes), true.pureSP[F]) {
              for {
                votedNodes <- nodesVotedNomination(nodeId, slotIndex, n)
                x          <- isQuorum(nodeId, votedNodes ++ acceptedNodes)
              } yield x
            }
            _ <- ifThen(changed)(acceptNewNomination(nodeId, slotIndex, n))
          } yield pre || changed
        }
      }

      // then try to confirm local accepted votes to make a candidates
      def tryCandidate(xs: ValueSet): SP[F, StateChanged] = {
        xs.foldLeft(false.pureSP[F]) { (acc, n) =>
          for {
            pre           <- acc
            acceptedNodes <- nodesAcceptedNomination(nodeId, slotIndex, n)
            confirmed     <- isQuorum(nodeId, acceptedNodes)
            _             <- ifThen(confirmed)(candidateNewNomination(nodeId, slotIndex, n))
          } yield pre || confirmed
        }
      }

      // once candidates maked, bump to ballot protocol
      // plus, if no candidate maked as so far, try to vote from nomination message.
      def tryGetNewValueFromNomination(nom: Message.Nomination,
                                       round: Int): SP[F, Option[Value]] = {
        nom.allValues
          .foldLeft((Option.empty[Value], 0L).pureSP[F]) { (acc, n) =>
            for {
              pre <- acc
              validValue <- ifM(validateValue(nodeId, slotIndex, n).map(_ == Value.Validity.FullyValidated),
                                Option(n).pureSP[F])(extractValidValue(nodeId, slotIndex, n))
              next <- ifM(validValue.isEmpty, pre) {
                for {
                  p <- hashValue(slotIndex, previousValue, round, validValue.get)
                } yield if (pre._1.isEmpty || p >= pre._2) (validValue, p) else pre
              }
            } yield next
          }
          .map(_._1)
      }

      val nom = statement.message
      for {
        toVotes      <- notAcceptedNominatingValues(nodeId, slotIndex, nom.voted)
        acceptNew    <- tryAcceptVotes(toVotes)
        accepted     <- acceptedNominations(nodeId, slotIndex)
        candidateNew <- tryCandidate(accepted)
        voteNew <- ifM(haveCandidateNominations(nodeId, slotIndex), false.pureSP[F]) {
          for {
            round <- currentNominateRound(nodeId, slotIndex)
            value <- tryGetNewValueFromNomination(nom, round)
            x <- ifM(value.isEmpty, false) {
              for {
                _ <- voteNewNominations(nodeId, slotIndex, ValueSet(value.get))
              } yield true
            }
          } yield x
        }
        _ <- ifThen(acceptNew || voteNew) {
          for {
            nomMsg <- createNominationMessage(nodeId, slotIndex)
            _      <- emit(nodeId, slotIndex, previousValue, nomMsg)
          } yield ()
        }
        _ <- ifThen(candidateNew) {
          for {
            candidates <- candidateNominations(nodeId, slotIndex)
            composite <- combineCandidates(nodeId, slotIndex, candidates)
            _         <- ifM(composite.isEmpty, false)(bumpState(nodeId, slotIndex, previousValue, composite.get, false))
          } yield ()
        }
      } yield true
    }
  }
}
