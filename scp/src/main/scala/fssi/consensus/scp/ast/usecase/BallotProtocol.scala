package fssi.consensus.scp.ast.usecase

import bigknife.sop._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types.Ballot.Phase
import fssi.consensus.scp.ast.domain.types.Statement.BallotStatement
import fssi.consensus.scp.ast.domain.types._

trait BallotProtocol[F[_]] extends BaseProtocol[F] {

  import model._

  def runBallotProtocol(slot: Slot,
                        envelope: Envelope): SP[F, (Slot, Envelope.State)] = {

    type Result = SP[F, (Slot, Envelope.State)]
    def invalid(slot: Slot): Result = (slot, Envelope.State.invalid).pureSP
    def valid(slot: Slot): Result = (slot, Envelope.State.valid).pureSP

    // first, do some checks:
    // 1. the statement should not be NominationStatement
    // 2. the statement related quorum set should be sane
    // 3. the BallotStatement should be sane
    // 4. the coming statement should be newer
    // 5. the values of the statement should be valid

    // check 2
    def isStatementRelativedQuorumSetSane(x: Statement): SP[F, Boolean] =
      for {
        quorumSet <- quorumSetService.resolveQuorumSetFromStatement(x)
        sane <- quorumSetService.isSane(quorumSet)
      } yield sane

    // check 3
    def isBallotStatementSane(x: Statement.BallotStatement): SP[F, Boolean] =
      for {
        sane <- statementService.isSaneBallotStatement(x)
      } yield sane

    // check4
    def isComingStatementNewer(x: Statement.BallotStatement): SP[F, Boolean] =
      for {
        oldEnvelope <- envelopeStore
          .findLatestEnvelope(slot.nodeID)
          .map(_.getOrElse(envelope))
        newer <- statementService.newerBallotStatement(
          oldEnvelope.statement.asInstanceOf[BallotStatement],
          x)
      } yield newer == x

    // check 5
    def isValidValue(
        slot: Slot,
        x: Statement.BallotStatement): SP[F, (Slot, Value.ValidationLevel)] =
      for {
        values <- statementService.valuesOfStatementToValidate(x)
        x <- values.foldRight(Value.fullyValid.pureSP[F]) { (n, acc) =>
          val res: SP[F, Value.ValidationLevel] = for {
            vl <- slotService.validateValueForBallot(slot, n)
            pre <- acc
          } yield
            if (vl != Value.ValidationLevel.FullyValidatedValue) vl else pre
          res
        }
        xSlot <- if (x == Value.maybeValid) slotService.setMaybeValid(slot)
        else slot.pureSP[F]
      } yield (xSlot, x)

    // todo how to write this in more elegant way?
    def check(next: (Slot, Statement.BallotStatement) => Result): Result =
      envelope.statement match {
        case x: Statement.BallotStatement =>
          isStatementRelativedQuorumSetSane(x) flatMap {
            case false => invalid(slot)
            case true =>
              isBallotStatementSane(x) flatMap {
                case false => invalid(slot)
                case true =>
                  isComingStatementNewer(x) flatMap {
                    case false => invalid(slot)
                    case true =>
                      isValidValue(slot, x) flatMap {
                        case (_, Value.ValidationLevel.InvalidValue) =>
                          invalid(slot)
                        case (xSlot, _) => next(xSlot, x)
                      }
                  }
              }
          }

        case _ => invalid(slot)
      }

    // record envelope
    def recordEnvelope(slot: Slot, x: Statement.BallotStatement): SP[F, Unit] =
      for {
        _ <- envelopeStore.saveLatestEnvelope(slot.nodeID, envelope)
        historicalStatement <- statementService.createHistoricalStatement(x)
        _ <- slotStore.saveHistoricalStatement(slot, historicalStatement)
      } yield ()

    // advance slot
    // 1. try to prepare accept, candidates -> accept candidates

    def findFederatedAccepted(slot: Slot, candidates: Set[Ballot]): SP[F, Option[Ballot]] = {
      candidates.foldRight((None: Option[Ballot]).pureSP[F]) { (n, acc) =>
        for {
          found <- acc
          _ <- if (found.isDefined) acc else {
            val passC1: SP[F, Boolean] = slot.unsafeBallotState.phase match {
              case Phase.Confirm =>
                for {
                  ret <- ballotService.areBallotsLessAndCompatible(slot.unsafeBallotState.prepared, n)
                }
              case _ => true.pureSP[F]
            }
          }
        } yield found
      }
    }
    def setPreparedAccept(ballot: Ballot): SP[F, Boolean] = ???
    // return boolean to say it has worked
    def attemptPreparedAccept(
        slot: Slot,
        statement: Statement.BallotStatement): SP[F, Boolean] =
      slot.unsafeBallotState.phase match {
        case Ballot.Phase.Externalize => false.pureSP[F]
        case _ =>
          for {
            candidates <- statementService.getPrepareCandidates(statement)
            acceptedOpt <- findFederatedAccepted(slot, candidates)
            x <- if (acceptedOpt.isDefined) setPreparedAccept(acceptedOpt.get) else false.pureSP[F]
          } yield x
      }
    def advanceSlot(slot: Slot,
                    statement: Statement.BallotStatement): SP[F, Slot] = ???

    // put together
    check { (slot, statement) =>
      slot.unsafeBallotState.phase match {
        case Ballot.Phase.Externalize =>
          for {
            workingBallot <- statementService.getWorkingBallot(statement)
            res <- if (workingBallot.value == slot.unsafeBallotState.commit.value) {
              for {
                _ <- recordEnvelope(slot, statement)
                x <- valid(slot)
              } yield x
            } else invalid(slot)
          } yield res

        case _ =>
          for {
            _ <- recordEnvelope(slot, statement)
            xSlot <- advanceSlot(slot, statement)
            res <- valid(xSlot)
          } yield res

      }
    }

    ???
  }
}
