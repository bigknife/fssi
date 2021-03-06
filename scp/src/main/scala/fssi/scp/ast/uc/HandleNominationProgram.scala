package fssi.scp
package ast
package uc

import types._
import components._
import bigknife.sop._
import bigknife.sop.implicits._
import fssi.scp.interpreter.store.NominationStatus

trait HandleNominationProgram[F[_]] extends SCP[F] with EmitProgram[F] {

  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._
  import model.logService._

  def handleNomination(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       previousValue: Value,
                       statement: Statement[Message.Nomination]): SP[F, Boolean] = {

    ifM(isNominatingStopped(slotIndex), false.pureSP[F]) {
      // try to accept votes from the message
      def tryAcceptVotes(xs: ValueSet): SP[F, StateChanged] = {
        xs.foldLeft(false.pureSP[F]) { (acc, n) =>
          for {
            pre           <- acc
            acceptedNodes <- nodesAcceptedNomination(slotIndex, n)
            accepted <- ifM(hasNominationValueAccepted(slotIndex, n), false.pureSP[F]) {
              for {
                agreed <- ifM(isLocalVBlocking(acceptedNodes), true.pureSP[F]) {
                  for {
                    votedNodes <- nodesVotedNomination(slotIndex, n)
                    x          <- isLocalQuorum(votedNodes ++ acceptedNodes)
                    _ <- if (x) debug(s"[$nodeId][$slotIndex] accepted $n by quorum")
                    else debug(s"[$nodeId][$slotIndex] did not accept $n by quorum")
                  } yield x
                }
              } yield agreed
            }
            _ <- ifThen(accepted) {
              for {
                _ <- info(s"[$nodeId][$slotIndex] accepted $n")
                _ <- acceptNewNomination(slotIndex, n)
              } yield ()
            }
          } yield pre || accepted
        }
      }

      // then try to confirm local accepted votes to make a candidates
      def tryCandidate(xs: ValueSet): SP[F, StateChanged] = {
        xs.foldLeft(false.pureSP[F]) { (acc, n) =>
          for {
            pre           <- acc
            acceptedNodes <- nodesAcceptedNomination(slotIndex, n)
            confirmed     <- isLocalQuorum(acceptedNodes)
            _ <- ifThen(confirmed) {
              for {
                _ <- info(s"[$nodeId][$slotIndex] confirmed nomination: $n")
                _ <- candidateNewNomination(slotIndex, n)
              } yield ()
            }
          } yield pre || confirmed
        }
      }

        // once candidates maked, bump to ballot protocol
        // plus, if no candidate maked as so far, try to vote from nomination message.

        val nom = statement.message
        for {
          toVotes   <- notAcceptedNominatingValues(slotIndex, nom.voted)
          acceptNew <- tryAcceptVotes(toVotes)
          _ <- if (acceptNew) info(s"[$nodeId][$slotIndex] accepted new votes: $toVotes")
          else info(s"[$nodeId][$slotIndex] votes: $toVotes not accepted")
          accepted     <- acceptedNominations(slotIndex)
          candidateNew <- tryCandidate(accepted)
          _ <- if (candidateNew)
            info(s"[$nodeId][$slotIndex] produced new candidate: $candidateNew")
          else info(s"[$nodeId][$slotIndex] no new candidates from votes: $toVotes")
          voteNew <- ifM(haveCandidateNominations(slotIndex), false.pureSP[F]) {
            ifM(isNotLeader(nodeId, slotIndex), false.pureSP[F]) {
              for {
                _     <- info(s"node $nodeId is leader on $slotIndex")
                round <- currentNominateRound(slotIndex)
                value <- tryGetNewValueFromNomination(nodeId, slotIndex, previousValue, nom, round)
                x <- ifM(value.isEmpty, false) {
                  for {
                    _ <- voteNewNominations(slotIndex, ValueSet(value.get))
                    _ <- info(
                      s"[$nodeId][$slotIndex] when no candidates, voted new nomination: ${value.get}")
                  } yield true
                }
              } yield x
            }
          }
          _ <- ifThen(acceptNew || voteNew) {
            for {
              nomMsg <- createNominationMessage(slotIndex)
              _      <- emitNomination(slotIndex, previousValue, nomMsg)
            } yield ()
          }
          _ <- ifThen(candidateNew) {
            for {
              candidates <- candidateNominations(slotIndex)
              composite  <- combineCandidates(nodeId, slotIndex, candidates)
              _ <- ifM(composite.isEmpty, false) {
                for {
                  _ <- info(
                    s"[$nodeId][$slotIndex] combined new composite value, un-forcefully bump to ballot: $composite")
                  _ <- candidateValueUpdated(slotIndex, composite.get)
                  x <- bumpState(slotIndex, previousValue, composite.get, force = false)
                } yield x
              }
            } yield ()
          }
          _ <- info(s"acceptNew: $acceptNew , voteNew: $voteNew , candidateNew: $candidateNew")
        } yield true
      }
    }

  private[uc] def tryGetNewValueFromNomination(nodeId: NodeID,
                                               slotIndex: SlotIndex,
                                               previousValue: Value,
                                               nom: Message.Nomination,
                                               round: Int): SP[F, Option[Value]] = {
    val nominationStatus: NominationStatus = NominationStatus.getInstance(slotIndex)

    nom.allValues
      .foldLeft((Option.empty[Value], 0L).pureSP[F]) { (acc, n) =>
        for {
          pre <- acc
          validValue <- ifM(
            validateValue(nodeId, slotIndex, n).map(_ == Value.Validity.FullyValidated),
            Option(n).pureSP[F])(extractValidValue(nodeId, slotIndex, n))
          next <- ifM(
            validValue.isEmpty || nominationStatus.votes.exists(_.contains(validValue.get)),
            pre) {
            for {
              p <- hashValue(slotIndex, previousValue, round, validValue.get)
            } yield if (pre._1.isEmpty || p >= pre._2) (validValue, p) else pre
          }
        } yield next
      }
      .map(_._1)
  }
}
