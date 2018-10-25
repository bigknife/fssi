package fssi.scp
package interpreter

import fssi.scp.ast._
import fssi.scp.interpreter.store.{BallotStatus, NominationStatus}
import fssi.scp.types._

import scala.collection.immutable.Set

class NodeStoreHandler extends NodeStore.Handler[Stack] {

  /** check the envelope to see if it's newer than local cache
    */
  override def isNewerEnvelope[M <: Message](nodeId: NodeID,
                                             slotIndex: SlotIndex,
                                             envelope: Envelope[M]): Stack[Boolean] = Stack {
    val r = envelope.statement.message match {
      case n: Message.Nomination =>
        val nominationStatus: NominationStatus = (nodeId, slotIndex)
        nominationStatus.latestNominations.map {
          _.get(nodeId) match {
            case Some(env) =>
              val oldMessage = env.statement.message
              val isDiff     = env != envelope
              val votedGrow = n.voted.size >= oldMessage.voted.size && oldMessage.voted.forall(
                n.voted.contains)
              val acceptedGrow = n.accepted.size >= oldMessage.accepted.size && oldMessage.accepted
                .forall(n.accepted.contains)
              isDiff && votedGrow && acceptedGrow
            case None => true
          }
        }
      case b: Message.BallotMessage =>
        val ballotStatus: BallotStatus = (nodeId, slotIndex)
        ballotStatus.latestEnvelopes.map {
          _.get(nodeId) match {
            case Some(env) =>
              val isDiff = b != env.statement.message
              val isGrow = b match {
                case newPrepare: Message.Prepare =>
                  env.statement.message match {
                    case oldPrepare: Message.Prepare =>
                      val compBallot = oldPrepare.b.compare(newPrepare.b)
                      if (compBallot < 0) true
                      else if (compBallot == 0) {
                        val prepareCompBallot = oldPrepare.p.compare(newPrepare.p)
                        if (prepareCompBallot < 0) true
                        else {
                          val preparePrimeCompBallot = oldPrepare.`p'`.compare(newPrepare.`p'`)
                          if (preparePrimeCompBallot < 0) true
                          else oldPrepare.`h.n` < newPrepare.`h.n`
                        }
                      } else false
                    case _ => false
                  }
                case newConf: Message.Confirm =>
                  env.statement.message match {
                    case _: Message.Prepare => true
                    case oldConf: Message.Confirm =>
                      val compBallot = oldConf.b.compare(newConf.b)
                      if (compBallot < 0) true
                      else if (compBallot == 0) {
                        if (oldConf.`p.n` == newConf.`p.n`) oldConf.`h.n` < newConf.`h.n`
                        else oldConf.`p.n` < newConf.`p.n`
                      } else false
                    case _: Message.Externalize => false
                  }
                case _: Message.Externalize => false
              }
              isDiff && isGrow
            case None => true
          }
        }
    }
    r.unsafe()
  }

  /** save new envelope, if it's a nomination message, save it into NominationStorage,
    * if it's a ballot message, save it into BallotStorage.
    */
  override def saveEnvelope[M <: Message](nodeId: NodeID,
                                          slotIndex: SlotIndex,
                                          envelope: Envelope[M]): Stack[Unit] = Stack {
    envelope.statement.message match {
      case n: Message.Nomination =>
        val nominationStatus: NominationStatus = (nodeId, slotIndex)
        nominationStatus.latestNominations := nominationStatus.latestNominations
          .unsafe() + (nodeId -> envelope.copy(statement = envelope.statement.withMessage(n)))
        ()
      case b: Message.BallotMessage =>
        val ballotStatus: BallotStatus = (nodeId, slotIndex)
        ballotStatus.latestEnvelopes := ballotStatus.latestEnvelopes
          .unsafe() + (nodeId -> envelope.copy(statement = envelope.statement.withMessage(b)))
        ()
    }
  }

  /** remove an envelope
    */
  override def removeEnvelope[M <: Message](nodeId: NodeID,
                                            slotIndex: SlotIndex,
                                            envelope: Envelope[M]): Stack[Unit] = Stack {
    envelope.statement.message match {
      case _: Message.Nomination =>
        val nominationStatus: NominationStatus = (nodeId, slotIndex)
        nominationStatus.latestNominations := nominationStatus.latestNominations.unsafe() - nodeId
        ()
      case _: Message.BallotMessage =>
        val ballotStatus: BallotStatus = (nodeId, slotIndex)
        ballotStatus.latestEnvelopes := ballotStatus.latestEnvelopes.unsafe() - nodeId
        ()
    }
  }

  /** find not accepted (nominate x) from values
    * @param values given a value set
    * @return a subset of values, the element in which is not accepted as nomination value
    */
  override def notAcceptedNominatingValues(nodeId: NodeID,
                                           slotIndex: SlotIndex,
                                           values: ValueSet): Stack[ValueSet] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    values.diff(nominationStatus.accepted.unsafe())
  }

  /** find current accepted nomination votes
    */
  override def acceptedNominations(nodeId: NodeID, slotIndex: SlotIndex): Stack[ValueSet] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    nominationStatus.accepted.unsafe()
  }

  /** find current candidates nomination value
    */
  override def candidateNominations(nodeId: NodeID, slotIndex: SlotIndex): Stack[ValueSet] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    nominationStatus.candidates.unsafe()
  }

  /** save new values to current voted nominations
    */
  override def voteNewNominations(nodeId: NodeID,
                                  slotIndex: SlotIndex,
                                  newVotes: ValueSet): Stack[Unit] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    nominationStatus.votes := nominationStatus.votes.map(_ ++ newVotes).unsafe(); ()
  }

  /** the set of nodes which have voted(nominate x)
    */
  override def nodesVotedNomination(nodeId: NodeID,
                                    slotIndex: SlotIndex,
                                    value: Value): Stack[Set[NodeID]] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    nominationStatus.latestNominations
      .map(map => map.keySet.filter(n => map(n).statement.message.voted.contains(value)))
      .unsafe()
  }

  /** the set of nodes which have accept(nominate x)
    */
  override def nodesAcceptedNomination(nodeId: NodeID,
                                       slotIndex: SlotIndex,
                                       value: Value): Stack[Set[NodeID]] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    nominationStatus.latestNominations
      .map(map => map.keySet.filter(n => map(n).statement.message.accepted.contains(value)))
      .unsafe()
  }

  /** save a new value to current accepted nominations
    */
  override def acceptNewNomination(nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   value: Value): Stack[Unit] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    nominationStatus.accepted := nominationStatus.accepted.map(_ + value).unsafe(); ()
  }

  /** save a new value to current candidated nominations
    */
  override def candidateNewNomination(nodeId: NodeID,
                                      slotIndex: SlotIndex,
                                      value: Value): Stack[Unit] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    nominationStatus.candidates := nominationStatus.candidates.map(_ + value).unsafe(); ()
  }

  /** get current ballot
    */
  override def currentBallot(nodeId: NodeID, slotIndex: SlotIndex): Stack[Option[Ballot]] = Stack {
    val ballotStatus: BallotStatus = (nodeId, slotIndex)
    ballotStatus.currentBallot.map(b => if (b.isBottom) None else Some(b)).unsafe()
  }

  /** given a ballot, get next ballot to try base on local state (z)
    * if there is a value stored in z, use <z, counter>, or use <attempt, counter>
    */
  override def nextBallotToTry(nodeId: NodeID,
                               slotIndex: SlotIndex,
                               attempt: Value,
                               counter: Int): Stack[Ballot] = Stack {
    val ballotStatus: BallotStatus = (nodeId, slotIndex)
    ballotStatus.valueOverride
      .map {
        case Some(v) => Ballot(counter, v)
        case None    => Ballot(counter, attempt)
      }
      .unsafe()
  }

  /** update local state when a new ballot was bumped into
    * @see BallotProtocol.cpp#399
    */
  override def updateBallotStateWhenBumpNewBallot(nodeId: NodeID,
                                                  slotIndex: SlotIndex,
                                                  newB: Ballot): Stack[Boolean] = Stack {
    val ballotStatus: BallotStatus = (nodeId, slotIndex)
    ballotStatus.phase.unsafe() match {
      case Ballot.Phase.Externalize => false
      case _ =>
        val isCurrentBallotExisted = ballotStatus.currentBallot.unsafe().isBottom
        val updated = if (!isCurrentBallotExisted) {
          ballotStatus.heardFromQuorum := false
          // TODO: start ballot protocol ?
          ballotStatus.currentBallot := newB
          val highBallot = ballotStatus.highBallot.unsafe()
          if (highBallot.nonEmpty && !newB.isCompatible(highBallot.get)) {
            ballotStatus.highBallot := None
          }
          true
        } else {
          val commit = ballotStatus.commit.unsafe()
          if (commit.nonEmpty && !commit.get.isCompatible(newB)) false
          else {
            if (ballotStatus.currentBallot.unsafe().counter != newB.counter) {
              ballotStatus.heardFromQuorum := false
            }
            ballotStatus.currentBallot := newB
            val highBallot = ballotStatus.highBallot.unsafe()
            if (highBallot.nonEmpty && !newB.isCompatible(highBallot.get)) {
              ballotStatus.highBallot := None
            }
            true
          }
        }
        val currentBallot = ballotStatus.currentBallot.unsafe()
        val prepared      = ballotStatus.prepared.unsafe()
        val preparedPrim  = ballotStatus.preparedPrime.unsafe()
        val commit        = ballotStatus.commit.unsafe()
        val highBallot    = ballotStatus.highBallot.unsafe()
        def currentBallotValid: Boolean = {
          if (isCurrentBallotExisted) currentBallot.counter != 0 else true
        }
        def prepareValid: Boolean = {
          if (prepared.nonEmpty && preparedPrim.nonEmpty)
            preparedPrim.get.isLess(prepared.get) && preparedPrim.get.isCompatible(prepared.get)
          else true
        }
        def highValid: Boolean = {
          if (highBallot.nonEmpty)
            !currentBallot.isBottom && highBallot.get.isLess(currentBallot) && highBallot.get
              .isCompatible(currentBallot)
          else true
        }
        def commitValid: Boolean = {
          if (commit.nonEmpty)
            !currentBallot.isBottom && highBallot.nonEmpty && commit.get
              .isLess(highBallot.get) && commit.get.isCompatible(highBallot.get) && highBallot.get
              .isLess(currentBallot) && highBallot.get.isCompatible(currentBallot)
          else true
        }
        val phaseValid: Boolean = ballotStatus.phase.unsafe() match {
          case Ballot.Phase.Prepare     => true
          case Ballot.Phase.Confirm     => commit.nonEmpty
          case Ballot.Phase.Externalize => commit.nonEmpty && highBallot.nonEmpty
        }
        updated && currentBallotValid && prepareValid && highValid && commitValid && phaseValid
    }
  }

  /** update local state when a ballot would be accepted as being prepared
    * @see BallotProtocol.cpp#879
    */
  override def updateBallotStateWhenAcceptPrepare(nodeId: NodeID,
                                                  slotIndex: SlotIndex,
                                                  newP: Ballot): Stack[Boolean] = ???

  /** update local state when a new high ballot and a new low ballot would be confirmed as being prepared
    * @see BallotProtocol.cpp#1031
    */
  override def updateBallotStateWhenConfirmPrepare(nodeId: NodeID,
                                                   slotIndex: SlotIndex,
                                                   newH: Option[Ballot],
                                                   newC: Option[Ballot]): Stack[Boolean] = ???

  /** check received ballot envelope, find nodes which are ahead of local node
    */
  override def nodesAheadLocal(nodeId: NodeID, slotIndex: SlotIndex): Stack[Set[NodeID]] = Stack {
    ???
  }

  /** find nodes ballot is ahead of a counter n
    * @see BallotProtocol#1385
    */
  override def nodesAheadBallotCounter(nodeId: NodeID,
                                       slotIndex: SlotIndex,
                                       counter: Int): Stack[Set[NodeID]] = ???

  /** set heard from quorum
    */
  override def heardFromQuorum(nodeId: NodeID, slotIndex: SlotIndex, heard: Boolean): Stack[Unit] =
    ???

  /** check heard from quorum
    */
  override def isHeardFromQuorum(nodeId: NodeID, slotIndex: SlotIndex): Stack[Boolean] = ???

  /** get current ballot phase
    */
  override def currentBallotPhase(nodeId: NodeID, slotIndex: SlotIndex): Stack[Ballot.Phase] =
    Stack {
      val ballotStatus: BallotStatus = (nodeId, slotIndex)
      ballotStatus.phase.unsafe()
    }

  /** get `c` in local state
    */
  override def currentCommitBallot(nodeId: NodeID, slotIndex: SlotIndex): Stack[Option[Ballot]] =
    Stack {
      val ballotStatus: BallotStatus = (nodeId, slotIndex)
      ballotStatus.commit.unsafe()
    }

  /** get current message level
    * message level is used to control `attempBump` only bening invoked once when advancing ballot which
    * would cause recursive-invoking.
    */
  override def currentMessageLevel(nodeId: NodeID, slotIndex: SlotIndex): Stack[Int] = Stack {
    val ballotStatus: BallotStatus = (nodeId, slotIndex)
    ballotStatus.currentMessageLevel.unsafe()
  }

  override def currentMessageLevelUp(nodeId: NodeID, slotIndex: SlotIndex): Stack[Unit] = Stack {
    val ballotStatus: BallotStatus = (nodeId, slotIndex)
    ballotStatus.currentMessageLevel := ballotStatus.currentMessageLevel.map(_ + 1).unsafe()
    ()
  }

  override def currentMessageLevelDown(nodeId: NodeID, slotIndex: SlotIndex): Stack[Unit] = Stack {
    val ballotStatus: BallotStatus = (nodeId, slotIndex)
    ballotStatus.currentMessageLevel := ballotStatus.currentMessageLevel.map(_ - 1).unsafe()
    ()
  }

  /** find all counters from received ballot message envelopes
    * @see BallotProtocol.cpp#1338
    */
  override def allCountersFromBallotEnvelopes(nodeId: NodeID,
                                              slotIndex: SlotIndex): Stack[CounterSet] = Stack {
    val ballotStatus: BallotStatus = (nodeId, slotIndex)
    val envelopeCounters = ballotStatus.latestEnvelopes
      .map {
        _.values.foldLeft(CounterSet.empty) { (acc, n) =>
          n.statement.message match {
            case p: Message.Prepare     => acc + p.b.counter
            case c: Message.Confirm     => acc + c.b.counter
            case _: Message.Externalize => acc + Int.MaxValue
          }
        }
      }
      .unsafe()
    envelopeCounters + ballotStatus.currentBallot.map(_.counter).unsafe()
  }

  /** get un emitted ballot message
    */
  override def currentUnEmittedBallotMessage(
      nodeId: NodeID,
      slotIndex: SlotIndex): Stack[Option[Message.BallotMessage]] = ???

  /** find candidate ballot to prepare from local stored envelopes received from other peers
    * if the ballot is prepared, should be ignored.
    * @see BallotProtocol.cpp#getPrepareCandidates
    */
  override def prepareCandidatesWithHint(nodeId: NodeID,
                                         slotIndex: SlotIndex,
                                         hint: Statement[Message.BallotMessage]): Stack[BallotSet] =
    Stack {
      val ballotStatus: BallotStatus = (nodeId, slotIndex)
      val hintBallots = hint.message match {
        case p: Message.Prepare =>
          BallotSet(Seq(Some(p.b), p.p, p.`p'`).filter(_.isDefined).map(_.get): _*)
        case c: Message.Confirm =>
          BallotSet(Ballot(c.`p.n`, c.b.value), Ballot(Int.MaxValue, c.b.value))
        case e: Message.Externalize =>
          if (e.commitableBallot.isDefined)
            BallotSet(Ballot(Int.MaxValue, e.commitableBallot.get.value))
          else BallotSet.empty
      }
      hintBallots.foldLeft(BallotSet.empty) { (acc, top) =>
        val preparedBallots =
          ballotStatus.latestEnvelopes.map(_.values).unsafe().foldLeft(BallotSet.empty) { (a, n) =>
            n.statement.message match {
              case prep: Message.Prepare =>
                val bc =
                  if (prep.b.isLess(top) && prep.b.isCompatible(top)) a + prep.b
                  else a
                val bp =
                  if (prep.p.nonEmpty && prep.p.get.isLess(top) && prep.p.get.isCompatible(top))
                    bc + prep.p.get
                  else bc
                val bpd =
                  if (prep.`p'`.nonEmpty && prep.`p'`.get.isLess(top) && prep.`p'`.get.isCompatible(
                        top))
                    bp + prep.`p'`.get
                  else bp
                bpd
              case confirm: Message.Confirm =>
                if (top.isCompatible(confirm.b)) {
                  val bt = a + top
                  if (confirm.`p.n` < top.counter) bt + Ballot(confirm.`p.n`, top.value)
                  else bt
                } else a
              case ext: Message.Externalize =>
                if (ext.commitableBallot.nonEmpty && top.isCompatible(top)) a + top
                else a
            }
          }
        acc ++ preparedBallots
      }
    }

  /** the set of nodes which have vote(prepare b)
    * @see BallotProtocol.cpp#839-866
    */
  override def nodesVotedPrepare(nodeId: NodeID,
                                 slotIndex: SlotIndex,
                                 ballot: Ballot): Stack[Set[NodeID]] = Stack {
    ???
  }

  /** the set of nodes which have accepted(prepare b)
    * @see BallotProtocol.cpp#1521
    */
  override def nodesAcceptedPrepare(nodeId: NodeID,
                                    slotIndex: SlotIndex,
                                    ballot: Ballot): Stack[Set[NodeID]] = Stack {
    val ballotStatus: BallotStatus = (nodeId, slotIndex)
    ballotStatus.latestEnvelopes
      .map(map =>
        map.keySet.filter { n =>
          map(n).statement.message match {
            case prep: Message.Prepare =>
              val prepared = prep.p.nonEmpty && ballot.isLess(prep.p.get) && ballot.isCompatible(
                prep.p.get)
              val preparedPrim = prep.`p'`.nonEmpty && ballot.isLess(prep.`p'`.get) && ballot
                .isCompatible(prep.`p'`.get)
              prepared && preparedPrim
            case confirm: Message.Confirm =>
              val prepared = Ballot(confirm.`p.n`, confirm.b.value)
              ballot.isLess(prepared) && ballot.isCompatible(prepared)
            case ext: Message.Externalize =>
              ext.commitableBallot.nonEmpty && ballot.isCompatible(ext.commitableBallot.get)
          }
      })
      .unsafe()
  }

  /** find all the commitable counters in recieved envelopes
    * @see BallotProtocol.cpp#1117
    */
  override def commitBoundaries(nodeId: NodeID,
                                slotIndex: SlotIndex,
                                ballot: Ballot): Stack[CounterSet] = Stack {
    val ballotStatus: BallotStatus = (nodeId, slotIndex)
    ballotStatus.latestEnvelopes
      .map(_.values.foldLeft(CounterSet.empty) { (acc, n) =>
        n.statement.message match {
          case prep: Message.Prepare =>
            if (ballot.isCompatible(prep.b) && prep.`c.n` != 0)
              acc ++ CounterSet(prep.`c.n`, prep.`h.n`)
            else acc
          case confirm: Message.Confirm =>
            if (ballot.isCompatible(confirm.b)) acc ++ CounterSet(confirm.`c.n`, confirm.`h.n`)
            else acc
          case ext: Message.Externalize =>
            if (ext.commitableBallot.nonEmpty && ballot.isCompatible(ext.commitableBallot.get))
              acc ++ CounterSet(ext.commitableBallot.get.counter, ext.`h.n`, Int.MaxValue)
            else acc
        }
      })
      .unsafe()
  }

  /** the set of nodes which have voted vote(commit b)
    */
  override def nodesVotedCommit(nodeId: NodeID,
                                slotIndex: SlotIndex,
                                ballot: Ballot,
                                counterInterval: CounterInterval): Stack[Set[NodeID]] = ???

  /** the set of nodes which have accepted vote(commit b)
    */
  override def nodesAcceptedCommit(nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   ballot: Ballot,
                                   counterInterval: CounterInterval): Stack[Set[NodeID]] = ???

  /** accept ballots(low and high) as committed
    * @see BallotProtocol.cpp#1292
    */
  override def acceptCommitted(nodeId: NodeID,
                               slotIndex: SlotIndex,
                               lowest: Ballot,
                               highest: Ballot): Stack[StateChanged] = ???

  /** confirm ballots(low and high) as committed
    * @see BallotProtocol.cpp#1292
    */
  override def confirmCommitted(nodeId: NodeID,
                                slotIndex: SlotIndex,
                                lowest: Ballot,
                                highest: Ballot): Stack[StateChanged] = ???

  /** check if it's able to accept commit a ballot now
    * @see BallotProtocol.cpp#1169-1172, 1209-1215
    */
  override def canAcceptCommitNow(nodeId: NodeID,
                                  slotIndex: SlotIndex,
                                  ballot: Ballot): Stack[Boolean] = ???

  /** check if it's able to confirm commit a ballot now
    * @see BallotProtocol.cpp#1434-1443, 1470-1473
    */
  override def canConfirmCommitNow(nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   ballot: Ballot): Stack[Boolean] = ???

  /** get current confirmed ballot
    */
  override def currentConfirmedBallot(nodeId: NodeID, slotIndex: SlotIndex): Stack[Ballot] = ???

  /** get current nominating round
    */
  override def currentNominateRound(nodeId: NodeID, slotIndex: SlotIndex): Stack[Int] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    nominationStatus.roundNumber.unsafe()
  }

  /** set nominate round to the next one
    */
  override def gotoNextNominateRound(nodeId: NodeID, slotIndex: SlotIndex): Stack[Unit] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    nominationStatus.roundNumber := nominationStatus.roundNumber.unsafe() + 1
    ()
  }

  /** save new values to current accepted nominations
    */
  override def acceptNewNominations(nodeId: NodeID,
                                    slotIndex: SlotIndex,
                                    values: ValueSet): Stack[Unit] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    nominationStatus.accepted := values
    ()
  }

  /** find latest candidate value
    */
  override def currentCandidateValue(nodeId: NodeID, slotIndex: SlotIndex): Stack[Option[Value]] =
    Stack {
      val nominationStatus: NominationStatus = (nodeId, slotIndex)
      nominationStatus.latestCompositeCandidate.unsafe()
    }

  /** check if a envelope can be emitted
    */
  override def canEmit[M <: Message](nodeId: NodeID,
                                     slotIndex: SlotIndex,
                                     envelope: Envelope[M]): Stack[Boolean] = Stack {
    envelope.statement.message match {
      case nominate: Message.Nomination =>
        val nominationStatus: NominationStatus = (nodeId, slotIndex)
        val lastEnvelopeOpt                    = nominationStatus.lastEnvelope.unsafe()
        if (lastEnvelopeOpt.isEmpty) true
        else {
          val lastEnvelope = lastEnvelopeOpt.get
          val timeLater    = envelope.statement.timestamp.value > lastEnvelope.statement.timestamp.value
          val votedGrow    = nominate.voted.size > lastEnvelope.statement.message.voted.size
          val acceptedGrow = nominate.accepted.size > lastEnvelope.statement.message.accepted.size
          timeLater && votedGrow && acceptedGrow
        }
      case ballot: Message.BallotMessage =>
        // TODO: handle ballot message envelope
        val ballotStatus: BallotStatus = (nodeId, slotIndex)
        ballot match {
          case prep: Message.Prepare    => true
          case confirm: Message.Confirm => true
          case ext: Message.Externalize => true
        }
    }
  }

  implicit def getNominationStatus(ns: (NodeID, SlotIndex)): NominationStatus = ns match {
    case (nodeId, slotIndex) => NominationStatus.getInstance(nodeId, slotIndex)
  }

  implicit def getBallotStatus(ns: (NodeID, SlotIndex)): BallotStatus = ns match {
    case (nodeId, slotIndex) => BallotStatus.getInstance(nodeId, slotIndex)
  }
}

object NodeStoreHandler {
  val instance = new NodeStoreHandler

  trait Implicits {
    implicit val scpNodeStoreHandler: NodeStoreHandler = instance
  }
}
