package fssi.consensus.scp.ast.usecase

import bigknife.sop._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types.Statement._
import fssi.consensus.scp.ast.domain.types._

trait NominationProtocol[F[_]] extends BaseProtocol[F] {

  import model._

  def runNominationProtocol(
      slot: Slot,
      envelope: Envelope): SP[F, (Slot, Envelope.State)] = {
    // for convenience, give a type alias for return type.
    type Result = SP[F, (Slot, Envelope.State)]

    // first check parameters
    // 1. should be Nominate statement
    // 2. coming Nominate should be newer
    def check(next: Nominate => Result): Result = {
      envelope.statement match {
        case x: Statement.Nominate =>
          for {
            sane <- statementService.isSane(x)
            state <- if (!sane) (slot, Envelope.State.invalid).pureSP[F]
            else
              for {
                nominateOpt <- statementStore.findLatestNomination(slot.nodeID)
                s1 <- if (nominateOpt.isEmpty) next(x)
                else
                  for {
                    newer <- statementService.newerNomination(x,
                                                              nominateOpt.get)
                    s0 <- if (newer == x) next(x)
                    else (slot, Envelope.State.invalid).pureSP[F]
                  } yield s0
              } yield s1
          } yield state

        case _ => (slot, Envelope.State.invalid).pureSP
      }
    }

    // then, record the coming envelope
    def recordEnvelope(nominate: Nominate): SP[F, Unit] =
      for {
        _ <- statementStore.updateLatestNomination(slot.nodeID, nominate)
        historicalStatement <- statementService.createHistoricalStatement(
          nominate)
        _ <- slotStore.saveHistoricalStatement(slot, historicalStatement)
      } yield ()

    // attempts to promote some of the votes to accepted
    def promoteVotes(nominate: Nominate, slot: Slot): SP[F, Slot] = {
      // promote a value (vote) to accepted
      def votedPredict(v: Value): Statement.Predict = Statement.predict {
        case x: Statement.Nominate => x.votes.contains(v)
        case _                     => false
      }
      def acceptPredict(v: Value): Statement.Predict = Statement.predict {
        case x: Statement.Nominate => x.accepted.contains(v)
        case _                     => false
      }
      def afterAccept(value: Value, slot: Slot): SP[F, Slot] =
        slotService.validateValue(value).flatMap {
          case Value.ValidationLevel.FullyValidatedValue =>
            slot.vote(value).accept(value).pureSP[F]
          case _ =>
            for {
              toVoteOpt <- slotService.extractValidValue(value)
            } yield toVoteOpt.map(slot.vote).getOrElse(slot)
        }

      def _promote(value: Value, slot: Slot): SP[F, Slot] =
        for {
          envs <- statementStore.latestNominations()
          accepted <- federatedAccept(votedPredict(value),
                                      acceptPredict(value),
                                      envs)
          xSlot <- if (!accepted) slot.pureSP[F] else afterAccept(value, slot)
        } yield xSlot

      // traverse the votes, if the value has been accepted, pass it, or _promote it.
      nominate.votes.foldLeft(slot.pureSP[F]) { (acc, n) =>
        for {
          xSlot <- acc
          ySlot <- if (xSlot.accepted.contains(n)) acc else _promote(n, xSlot)
        } yield ySlot
      }
    }

    // attempt to promote some of accepted values to candidates
    def promoteAccepted(nominate: Nominate, slot: Slot): SP[F, Slot] = {
      def acceptPredict(v: Value): Statement.Predict = Statement.predict {
        case x: Statement.Nominate => x.accepted.contains(v)
        case _                     => false
      }

      def _promote(value: Value, slot: Slot): SP[F, Slot] =
        for {
          envs <- statementStore.latestNominations()
          ratified <- federatedRatify(acceptPredict(value), envs)
          xSlot <- if (ratified) slot.candidate(value).pureSP[F]
          else slot.pureSP[F]
        } yield xSlot

      nominate.accepted.foldLeft(slot.pureSP[F]) { (acc, n) =>
        for {
          xSlot <- acc
          ySlot <- if (xSlot.candidates.contains(n)) acc else _promote(n, xSlot)
        } yield ySlot
      }
    }

    // only take round leader votes if we're still looking for candidates
    def voteRoundLeader(nominate: Nominate, slot: Slot): SP[F, Slot] = {
      def getNewValue(nominate: Nominate): SP[F, Option[Value]] = {
        // pick the highest value we don't have from the leader
        // sorted using hashValue
        // init value: optional value and hash
        val initValue: SP[F, (Option[Value], Long)] =
          (None: Option[Value], 0L).pureSP[F]

        val finalValue: SP[F, (Option[Value], Long)] =
          (nominate.votes ++ nominate.accepted).foldLeft(initValue) {
            (acc, n) =>
              val valueToNominateOptEff: SP[F, Option[Value]] =
                slotService.validateValue(n).flatMap {
                  case Value.ValidationLevel.FullyValidatedValue =>
                    Option(n).pureSP[F]
                  case _ => slotService.extractValidValue(n)
                }
              for {
                pre <- acc
                valueToNominateOpt <- valueToNominateOptEff
                next <- if (valueToNominateOpt.isDefined && !slot.votes
                              .contains(valueToNominateOpt.get)) {

                  val n: SP[F, (Option[Value], Long)] = for {
                    curHash <- slotService.hashValue(slot,
                                                     valueToNominateOpt.get)
                  } yield {
                    if (curHash >= pre._2)
                      (Option(valueToNominateOpt.get), curHash)
                    else pre
                  }
                  n

                } else acc
              } yield next
          }

        finalValue.map(_._1)
      }

      if (slot.candidates.isEmpty && slot.roundLeaders.contains(
            nominate.nodeID)) {

        getNewValue(nominate).flatMap {
          case Some(newValue) => slotService.nominatingValue(slot, newValue)
          case _              => slot.pureSP
        }
      } else slot.pureSP[F]
    }

    // if slot has been modified, proceed to run this protocol.

    def emitNomination(slot: Slot): SP[F, Slot] = {
      //def updateSlotLastEnvelope(envelope: Envelope): SP[F, Slot] = ???
      import bigknife.sop.implicits._
      for {
        newNominate <- statementService.createNomination(slot)
        newEnvelope <- envelopeService.signEnvelope(Envelope(newNominate))
        ns <- runNominationProtocol(slot, newEnvelope)
        xSlot <- if (ns._2 == Envelope.State.Valid) {
          val tmpNewEnvelope = ns._1.lastEnvelope.getOrElse(newEnvelope)
          val n: SP[F, Slot] = for {
            newer <- statementService.newerNomination(tmpNewEnvelope.statement.asInstanceOf[Nominate], newNominate)
          } yield if (newer == newNominate) ns._1.copy(lastEnvelope = Some(newEnvelope)) else ns._1
          n
        } else throw new RuntimeException("moved to a bad state(nomination)")
        _ <- slotService.emitEnvelope(xSlot, newEnvelope)
      } yield xSlot
    }

    // bumps the ballot
    def bumpState(slot: Slot): SP[F, Slot] = {
      for {
        value <- slotService.combineCandidates(slot)
        xSlot <- slotService.bumpState(slot, value, force = false)
      } yield xSlot
    }

    // do nothing
    // def doNothing(): SP[F, Unit] = ().pureSP[F]

    // put together
    check { nominate =>
      recordEnvelope(nominate)

      val n: SP[F, (Slot, Envelope.State)] = for {
        xSlot <- promoteVotes(nominate, slot)
        ySlot <- promoteAccepted(nominate, xSlot)
        zSlot <- voteRoundLeader(nominate, ySlot)
        change <- slotService.resolveChange(slot, zSlot)
        finalSlot <- change match {
          case Slot.Change.Modified     => emitNomination(zSlot)
          case Slot.Change.NewCandidate => bumpState(zSlot)
          case _                        => zSlot.pureSP[F]
        }
      } yield (finalSlot, Envelope.State.valid)

      n
    }
  }
}
