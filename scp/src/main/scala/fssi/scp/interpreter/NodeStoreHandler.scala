package fssi.scp
package interpreter

import fssi.scp.ast._
import fssi.scp.interpreter.store.{BallotStatus, NominationStatus, Var}
import fssi.scp.types.Message.{BallotMessage, Nomination}
import fssi.scp.types._

import scala.collection.immutable.Set

class NodeStoreHandler extends NodeStore.Handler[Stack] with LogSupport {

  /** check the envelope to see if it's newer than local cache
    */
  override def isNewerEnvelope[M <: Message](nodeId: NodeID,
                                             slotIndex: SlotIndex,
                                             envelope: Envelope[M]): Stack[Boolean] = Stack {
    setting =>
      val r = envelope.statement.message match {
        case n: Message.Nomination =>
          val nominationStatus: NominationStatus = slotIndex
          nominationStatus.latestNominations.map {
            _.get(nodeId) match {
              case _
                  if n.voted.size == 1 && n.voted.headOption
                    .exists(_.rawBytes.length == 0) && setting.localNode == nodeId =>
                true
              case Some(env) =>
//              val oldMessage = env.statement.message
//              val isDiff     = env != envelope
//              val votedGrow = n.voted.size >= oldMessage.voted.size && oldMessage.voted.forall(
//                n.voted.contains)
//              val acceptedGrow = n.accepted.size >= oldMessage.accepted.size && oldMessage.accepted
//                .forall(n.accepted.contains)
//              isDiff && votedGrow && acceptedGrow
                n.isNewerThan(env.statement.message)
              case None => true
            }
          }
        case coming: Message.BallotMessage =>
          val ballotStatus: BallotStatus = slotIndex

          ballotStatus.latestEnvelopes.map {
            _.get(nodeId) match {
              case Some(old) =>
                isNewerBallotMessage(coming, old.statement.message)
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
                                          envelope: Envelope[M]): Stack[Unit] = Stack { setting =>
    envelope.statement.message match {
      case n: Message.Nomination =>
        val nominationStatus: NominationStatus = slotIndex
        nominationStatus.latestNominations := nominationStatus.latestNominations
          .unsafe() + (nodeId -> envelope.copy(statement = envelope.statement.withMessage(n)))
        log.info(
          s"current latest nominations --> ${nominationStatus.latestNominations.unsafe().keys.size}")
        nominationStatus.latestNominations.foreach { map =>
          map.keySet.foreach { node =>
            log.info(
              s"node $node vote --> ${map(node).statement.message.voted.size}, accept --> ${map(
                node).statement.message.accepted.size}")
          }
        }
        ()
      case b: Message.BallotMessage =>
        val ballotStatus: BallotStatus = slotIndex
        ballotStatus.latestEnvelopes := ballotStatus.latestEnvelopes
          .unsafe() + (nodeId -> envelope.copy(statement = envelope.statement.withMessage(b)))
        ()
    }
  }

  /** get nomination envelope from locally stored, received from peer nodes
    */
  override def getNominationEnvelope(
      slotIndex: SlotIndex,
      peerNodeId: NodeID): Stack[Option[Envelope[Message.Nomination]]] = Stack {
    val nominationStatus: NominationStatus = slotIndex
    nominationStatus.latestNominations.map(_.get(peerNodeId)).unsafe()
  }

  override def nominationsReceived(
      slotIndex: SlotIndex): Stack[Map[NodeID, Envelope[Message.Nomination]]] = Stack {
    val nominationStatus: NominationStatus = slotIndex
    nominationStatus.latestNominations.unsafe()
  }

  /** remove an envelope
    */
  override def removeEnvelope[M <: Message](nodeId: NodeID,
                                            slotIndex: SlotIndex,
                                            envelope: Envelope[M]): Stack[Unit] = Stack {
    envelope.statement.message match {
      case _: Message.Nomination =>
        val nominationStatus: NominationStatus = slotIndex
        nominationStatus.latestNominations := nominationStatus.latestNominations.unsafe() - nodeId
        ()
      case _: Message.BallotMessage =>
        val ballotStatus: BallotStatus = slotIndex
        ballotStatus.latestEnvelopes := ballotStatus.latestEnvelopes.unsafe() - nodeId
        ()
    }
  }

  /** find not accepted (nominate x) from values
    *
    * @param values given a value set
    * @return a subset of values, the element in which is not accepted as nomination value
    */
  override def notAcceptedNominatingValues(slotIndex: SlotIndex,
                                           values: ValueSet): Stack[ValueSet] = Stack {
    val nominationStatus: NominationStatus = slotIndex
    values.diff(nominationStatus.accepted.unsafe())
  }

  /** find current accepted nomination votes
    */
  override def acceptedNominations(slotIndex: SlotIndex): Stack[ValueSet] = Stack {
    val nominationStatus: NominationStatus = slotIndex
    nominationStatus.accepted.unsafe()
  }

  override def hasNominationValueAccepted(slotIndex: SlotIndex, value: Value): Stack[Boolean] =
    Stack {
      val nominationStatus: NominationStatus = slotIndex
      nominationStatus.accepted.unsafe().contains(value)
    }

  /** find current candidates nomination value
    */
  override def candidateNominations(slotIndex: SlotIndex): Stack[ValueSet] = Stack {
    val nominationStatus: NominationStatus = slotIndex
    nominationStatus.candidates.unsafe()
  }

  /** save new values to current voted nominations
    */
  override def voteNewNominations(slotIndex: SlotIndex, newVotes: ValueSet): Stack[Unit] = Stack {
    val nominationStatus: NominationStatus = slotIndex
    nominationStatus.votes := nominationStatus.votes.map(_ ++ newVotes).unsafe(); ()
  }

  /** the set of nodes which have voted(nominate x)
    */
  override def nodesVotedNomination(slotIndex: SlotIndex, value: Value): Stack[Set[NodeID]] =
    Stack {
      val nominationStatus: NominationStatus = slotIndex
      nominationStatus.latestNominations
        .map(map => map.keySet.filter(n => map(n).statement.message.voted.contains(value)))
        .unsafe()
    }

  /** the set of nodes which have accept(nominate x)
    */
  override def nodesAcceptedNomination(slotIndex: SlotIndex, value: Value): Stack[Set[NodeID]] =
    Stack {
      val nominationStatus: NominationStatus = slotIndex
      nominationStatus.latestNominations
        .map(map => map.keySet.filter(n => map(n).statement.message.accepted.contains(value)))
        .unsafe()
    }

  /** save a new value to current accepted nominations
    */
  override def acceptNewNomination(slotIndex: SlotIndex, value: Value): Stack[Unit] = Stack {
    setting =>
      val nominationStatus: NominationStatus = slotIndex
      if (setting.applicationCallback
            .validateValue(setting.localNode, slotIndex, value) == Value.Validity.FullyValidated)
        nominationStatus.votes := nominationStatus.votes.map(_ + value).unsafe()

      nominationStatus.accepted := nominationStatus.accepted.map(_ + value).unsafe(); ()
  }

  /** save a new value to current candidated nominations
    */
  override def candidateNewNomination(slotIndex: SlotIndex, value: Value): Stack[Unit] = Stack {
    val nominationStatus: NominationStatus = slotIndex
    nominationStatus.candidates := nominationStatus.candidates.map(_ + value).unsafe(); ()
    log.info(s"candidate size --> ${nominationStatus.candidates.unsafe().size}")
  }

  override def isLeader(nodeID: NodeID, slotIndex: SlotIndex): Stack[Boolean] = Stack {
    val nominationStatus: NominationStatus = NominationStatus.getInstance(slotIndex)
    nominationStatus.roundLeaders.exists(_.contains(nodeID))
  }

  /** get current ballot
    */
  override def currentBallot(slotIndex: SlotIndex): Stack[Option[Ballot]] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.currentBallot.map(b => if (b.isBottom) None else Some(b)).unsafe()
  }

  /** given a ballot, get next ballot to try base on local state (z)
    * if there is a value stored in z, use <z, counter>, or use <attempt, counter>
    */
  override def nextBallotToTry(slotIndex: SlotIndex, attempt: Value, counter: Int): Stack[Ballot] =
    Stack {
      val ballotStatus: BallotStatus = slotIndex
      ballotStatus.valueOverride
        .map {
          case Some(v) => Ballot(counter, v)
          case _       => Ballot(counter, attempt)
        }
        .unsafe()
    }

  /** update local state when a new ballot was bumped into
    *
    * @see BallotProtocol.cpp::updateCurrentValue#399
    */
  override def updateBallotStateWhenBumpNewBallot(slotIndex: SlotIndex,
                                                  newB: Ballot): Stack[Boolean] = Stack {
    val ballotStatus: BallotStatus = slotIndex

    ballotStatus.phase.unsafe() match {
      case Ballot.Phase.Externalize => false

      case _ =>
        val currentBallot = ballotStatus.currentBallot.unsafe()

        val updated = if (currentBallot.isBottom) {
          bumpToBallot(slotIndex, newB, check = true)
        } else {
          val commit = ballotStatus.commit.unsafe()

          if (commit.nonEmpty && !commit.get.compatible(newB)) false
          else {
            val comp = currentBallot.compare(newB)
            if (comp < 0) bumpToBallot(slotIndex, newB, check = true)
            else false
          }
        }

        updated && checkInvariants(slotIndex)
    }
  }

  /** update local state when a ballot would be accepted as being prepared
    *
    * @see BallotProtocol.cpp#879
    */
  override def updateBallotStateWhenAcceptPrepare(slotIndex: SlotIndex,
                                                  newP: Ballot): Stack[Boolean] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    val preparedOpt                = ballotStatus.prepared.unsafe()
    val preparedPrimOpt            = ballotStatus.preparedPrime.unsafe()

    val didWork = if (preparedOpt.nonEmpty) {

      val comp = preparedOpt.get.compare(newP)

      if (comp < 0) {
        if (!preparedOpt.get.compatible(newP)) ballotStatus.preparedPrime := preparedOpt
        ballotStatus.prepared := Option(newP); true
      } else if (comp > 0) {
        if (preparedPrimOpt.isEmpty || preparedPrimOpt.get.compare(newP) < 0) {
          ballotStatus.preparedPrime := Option(newP); true
        } else false
      } else false
    } else {
      ballotStatus.prepared := Option(newP); true
    }

    val commitOpt     = ballotStatus.commit.unsafe()
    val highBallotOpt = ballotStatus.highBallot.unsafe()

    def higherAndInCompatible(p: Option[Ballot], h: Ballot): Boolean = (p, h) match {
      case (Some(prepare), h) => h.isLess(prepare) && !h.compatible(prepare)
      case _                  => false
    }

    val updated = if (commitOpt.nonEmpty && highBallotOpt.nonEmpty) {

      val preparedValid = higherAndInCompatible(ballotStatus.prepared.unsafe(), highBallotOpt.get)

      val preparedPrimValid =
        higherAndInCompatible(ballotStatus.preparedPrime.unsafe(), highBallotOpt.get)

      if ((preparedValid || preparedPrimValid) && ballotStatus.phase
            .unsafe() == Ballot.Phase.Prepare) {
        ballotStatus.commit := None; true
      } else false

    } else false

    didWork || updated
  }

  /** update local state when a new high ballot and a new low ballot would be confirmed as being prepared
    *
    * @see BallotProtocol.cpp#1031
    */
  override def updateBallotStateWhenConfirmPrepare(slotIndex: SlotIndex,
                                                   newH: Option[Ballot],
                                                   newC: Option[Ballot]): Stack[Boolean] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.valueOverride := newH.map(_.value)
    val currentBallot = ballotStatus.currentBallot.unsafe()
    val highBallotOpt = ballotStatus.highBallot.unsafe()

    val didWork =
      if (currentBallot.isBottom || (newH.nonEmpty && currentBallot.compatible(newH.get))) {
        val highUpdated = if (highBallotOpt.isEmpty || newH.compare(highBallotOpt) > 0) {
          ballotStatus.highBallot := newH; true
        } else false

        val commitUpdated =
          if (newC.nonEmpty && newC.get.counter != 0) {
            ballotStatus.commit := newC; true
          } else false

        highUpdated || commitUpdated
      } else false

    val currentUpdated = newH.nonEmpty && updateCurrentIfNeed(slotIndex, newH.get)
    didWork || currentUpdated
  }

  /** check received ballot envelope, find nodes which are ahead of local node
    */
  override def nodesAheadLocal(slotIndex: SlotIndex): Stack[Set[NodeID]] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.latestEnvelopes
      .map { map =>
        map.keySet.filter(n =>
          map(n).statement.message match {
            case prep: Message.Prepare =>
              ballotStatus.currentBallot.unsafe().counter <= prep.b.counter
            case _ => true
        })
      }
      .unsafe()
  }

  /** find nodes ballot is ahead of a counter n
    *
    * @see BallotProtocol#1385
    */
  override def nodesAheadBallotCounter(slotIndex: SlotIndex, counter: Int): Stack[Set[NodeID]] =
    Stack {
      val ballotStatus: BallotStatus = slotIndex
      ballotStatus.latestEnvelopes
        .map(map =>
          map.keySet.filter(n =>
            map(n).statement.message match {
              case prep: Message.Prepare    => counter < prep.b.counter
              case confirm: Message.Confirm => counter < confirm.b.counter
              case _: Message.Externalize   => counter != Int.MaxValue
          }))
        .unsafe()
    }

  /** set heard from quorum
    */
  override def heardFromQuorum(slotIndex: SlotIndex, heard: Boolean): Stack[Unit] =
    Stack {
      val ballotStatus: BallotStatus = slotIndex
      ballotStatus.heardFromQuorum := heard; ()
    }

  /** check heard from quorum
    */
  override def isHeardFromQuorum(slotIndex: SlotIndex): Stack[Boolean] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.heardFromQuorum.unsafe()
  }

  override def ballotDidHearFromQuorum(slotIndex: SlotIndex): Stack[Unit] = Stack { setting =>
    val ballotStatus: BallotStatus = slotIndex
    setting.applicationCallback.ballotDidHearFromQuorum(slotIndex,
                                                        ballotStatus.currentBallot.unsafe())
  }

  /** get current ballot phase
    */
  override def currentBallotPhase(slotIndex: SlotIndex): Stack[Ballot.Phase] =
    Stack {
      val ballotStatus: BallotStatus = slotIndex
      ballotStatus.phase.unsafe()
    }

  /** get `c` in local state
    */
  override def currentCommitBallot(slotIndex: SlotIndex): Stack[Option[Ballot]] =
    Stack {
      val ballotStatus: BallotStatus = slotIndex
      ballotStatus.commit.unsafe()
    }

  /** get current message level
    * message level is used to control `attempBump` only bening invoked once when advancing ballot which
    * would cause recursive-invoking.
    */
  override def currentMessageLevel(slotIndex: SlotIndex): Stack[Int] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.currentMessageLevel.unsafe()
  }

  override def currentMessageLevelUp(slotIndex: SlotIndex): Stack[Unit] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.currentMessageLevel := ballotStatus.currentMessageLevel.map(_ + 1).unsafe()
    ()
  }

  override def currentMessageLevelDown(slotIndex: SlotIndex): Stack[Unit] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.currentMessageLevel := ballotStatus.currentMessageLevel.map(_ - 1).unsafe()
    ()
  }

  /** find all counters from received ballot message envelopes
    *
    * @see BallotProtocol.cpp#1338
    */
  override def allCountersFromBallotEnvelopes(slotIndex: SlotIndex): Stack[CounterSet] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.latestEnvelopes
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
  }

  /** get un emitted ballot message
    */
  override def currentUnEmittedBallotMessage(
      slotIndex: SlotIndex): Stack[Option[Message.BallotMessage]] = Stack {
    val ballotStatus = BallotStatus.getInstance(slotIndex)
    (for {
      e <- ballotStatus.latestEmitEnvelope
      g <- ballotStatus.latestGeneratedEnvelope
    } yield if (g != e) Some(g.statement.message) else None).getOrElse(None)
  }

  /** find candidate ballot to prepare from local stored envelopes received from other peers
    * if the ballot is prepared, should be ignored.
    *
    * @see BallotProtocol.cpp#getPrepareCandidates
    */
  override def prepareCandidatesWithHint(slotIndex: SlotIndex,
                                         hint: Statement[Message.BallotMessage]): Stack[BallotSet] =
    Stack {
      val ballotStatus: BallotStatus = slotIndex
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
                  if (prep.b.isLess(top) && prep.b.compatible(top)) a + prep.b
                  else a
                val bp =
                  if (prep.p.nonEmpty && prep.p.get.isLess(top) && prep.p.get.compatible(top))
                    bc + prep.p.get
                  else bc
                val bpd =
                  if (prep.`p'`.nonEmpty && prep.`p'`.get.isLess(top) && prep.`p'`.get.compatible(
                        top))
                    bp + prep.`p'`.get
                  else bp
                bpd
              case confirm: Message.Confirm =>
                if (top.compatible(confirm.b)) {
                  val bt = a + top
                  if (confirm.`p.n` < top.counter) bt + Ballot(confirm.`p.n`, top.value)
                  else bt
                } else a
              case ext: Message.Externalize =>
                if (ext.commitableBallot.nonEmpty && top.compatible(top)) a + top
                else a
            }
          }
        acc ++ preparedBallots
      }
    }

  /** the set of nodes which have vote(prepare b)
    *
    * @see BallotProtocol.cpp#839-866
    */
  override def nodesVotedPrepare(slotIndex: SlotIndex, ballot: Ballot): Stack[Set[NodeID]] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.latestEnvelopes
      .map(map =>
        map.keySet.filter(n =>
          map(n).statement.message match {
            case prep: Message.Prepare    => ballot.isLess(prep.b) && ballot.compatible(prep.b)
            case confirm: Message.Confirm => ballot.compatible(confirm.b)
            case ext: Message.Externalize =>
              ext.commitableBallot.nonEmpty && ballot.compatible(ext.commitableBallot.get)
        }))
      .unsafe()
  }

  /** the set of nodes which have accepted(prepare b)
    *
    * @see BallotProtocol.cpp#1521
    */
  override def nodesAcceptedPrepare(slotIndex: SlotIndex, ballot: Ballot): Stack[Set[NodeID]] =
    Stack {
      val ballotStatus: BallotStatus = slotIndex
      ballotStatus.latestEnvelopes
        .map(map =>
          map.keySet.filter { n =>
            map(n).statement.message match {
              case prep: Message.Prepare =>
                val prepared = prep.p.nonEmpty && ballot.isLess(prep.p.get) && ballot.compatible(
                  prep.p.get)
                val preparedPrim = prep.`p'`.nonEmpty && ballot.isLess(prep.`p'`.get) && ballot
                  .compatible(prep.`p'`.get)
                prepared || preparedPrim
              case confirm: Message.Confirm =>
                val prepared = Ballot(confirm.`p.n`, confirm.b.value)
                ballot.isLess(prepared) && ballot.compatible(prepared)
              case ext: Message.Externalize =>
                ext.commitableBallot.nonEmpty && ballot.compatible(ext.commitableBallot.get)
            }
        })
        .unsafe()
    }

  /** find all the commitable counters in recieved envelopes
    *
    * @see BallotProtocol.cpp#1117
    */
  override def commitBoundaries(slotIndex: SlotIndex, ballot: Ballot): Stack[CounterSet] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.latestEnvelopes
      .map(_.values.foldLeft(CounterSet.empty) { (acc, n) =>
        n.statement.message match {
          case prep: Message.Prepare =>
            if (ballot.compatible(prep.b) && prep.`c.n` != 0)
              acc ++ CounterSet(prep.`c.n`, prep.`h.n`)
            else acc
          case confirm: Message.Confirm =>
            if (ballot.compatible(confirm.b)) acc ++ CounterSet(confirm.`c.n`, confirm.`h.n`)
            else acc
          case ext: Message.Externalize =>
            if (ext.commitableBallot.nonEmpty && ballot.compatible(ext.commitableBallot.get))
              acc ++ CounterSet(ext.commitableBallot.get.counter, ext.`h.n`, Int.MaxValue)
            else acc
        }
      })
      .unsafe()
  }

  /** the set of nodes which have voted vote(commit b)
    */
  override def nodesVotedCommit(slotIndex: SlotIndex,
                                ballot: Ballot,
                                counterInterval: CounterInterval): Stack[Set[NodeID]] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.latestEnvelopes
      .map(map =>
        map.keySet.filter(n =>
          map(n).statement.message match {
            case prep: Message.Prepare =>
              if (ballot.compatible(prep.b) && prep.`c.n` != 0) {
                prep.`c.n` <= counterInterval.first && counterInterval.second <= prep.`h.n`
              } else false
            case confirm: Message.Confirm =>
              if (ballot.compatible(confirm.b)) {
                confirm.`c.n` <= counterInterval.first
              } else false
            case ext: Message.Externalize =>
              if (ext.commitableBallot.nonEmpty && ballot.compatible(ext.commitableBallot.get)) {
                ext.commitableBallot.get.counter <= counterInterval.first
              } else false
        }))
      .unsafe()
  }

  /** the set of nodes which have accepted vote(commit b)
    */
  override def nodesAcceptedCommit(slotIndex: SlotIndex,
                                   ballot: Ballot,
                                   counterInterval: CounterInterval): Stack[Set[NodeID]] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.latestEnvelopes
      .map(map =>
        map.keySet.filter(n =>
          map(n).statement.message match {
            case _: Message.Prepare => false
            case confirm: Message.Confirm =>
              if (ballot.compatible(confirm.b)) {
                confirm.`c.n` <= counterInterval.first && counterInterval.second <= confirm.`h.n`
              } else false
            case ext: Message.Externalize =>
              if (ext.commitableBallot.nonEmpty && ballot.compatible(ext.commitableBallot.get)) {
                ext.commitableBallot.get.counter <= counterInterval.first
              } else false
        }))
      .unsafe()
  }

  /** accept ballots(low and high) as committed
    *
    * @see BallotProtocol.cpp#1292
    */
  override def acceptCommitted(slotIndex: SlotIndex,
                               lowest: Ballot,
                               highest: Ballot): Stack[StateChanged] = Stack {
    val ballotStatus: BallotStatus = slotIndex

    ballotStatus.valueOverride := Option(highest.value)

    val highestBallotOpt = ballotStatus.highBallot.unsafe()
    val commitBallotOpt  = ballotStatus.commit.unsafe()

    val highestUpdated =
      if (highestBallotOpt.isEmpty || commitBallotOpt.isEmpty || highestBallotOpt.get.compare(
            highest) != 0 || commitBallotOpt.get.compare(lowest) != 0) {
        ballotStatus.commit := Option(lowest)
        ballotStatus.highBallot := Option(highest)
        true
      } else false

    val phase         = ballotStatus.phase.unsafe()
    val currentBallot = ballotStatus.currentBallot.unsafe()

    val phaseUpdated = if (phase == Ballot.Phase.Prepare) {
      ballotStatus.phase := Ballot.Phase.Confirm

      val bumpUpdated =
        if (!currentBallot.isBottom && !(highest.isLess(currentBallot) && highest.compatible(
              currentBallot))) {
          bumpToBallot(slotIndex, highest, check = false)
        } else true

      ballotStatus.preparedPrime := None

      bumpUpdated
    } else false

    val didWork = highestUpdated || phaseUpdated

    if (didWork)
      updateCurrentIfNeed(slotIndex, ballotStatus.highBallot.unsafe().get)

    didWork
  }

  /** confirm ballots(low and high) as committed
    *
    * @see BallotProtocol.cpp#1292
    */
  override def confirmCommitted(slotIndex: SlotIndex,
                                lowest: Ballot,
                                highest: Ballot): Stack[StateChanged] = Stack {
    val ballotStatus: BallotStatus = slotIndex

    ballotStatus.commit := Option(lowest)
    ballotStatus.highBallot := Option(highest)

    updateCurrentIfNeed(slotIndex, highest)

    ballotStatus.phase := Ballot.Phase.Externalize

    true
  }

  /** check if it's able to accept commit a ballot now
    *
    * @see BallotProtocol.cpp#1169-1172, 1209-1215
    */
  override def canAcceptCommitNow(slotIndex: SlotIndex, ballot: Ballot): Stack[Boolean] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    val phase                      = ballotStatus.phase.unsafe()
    val highBallot                 = ballotStatus.highBallot.unsafe()
    if (phase == Ballot.Phase.Externalize) false
    else if (phase == Ballot.Phase.Confirm && (highBallot.isEmpty || !ballot.compatible(
               highBallot.get))) false
    else true
  }

  /** check if it's able to confirm commit a ballot now
    *
    * @see BallotProtocol.cpp#1434-1443, 1470-1473
    */
  override def canConfirmCommitNow(slotIndex: SlotIndex, ballot: Ballot): Stack[Boolean] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    val phase                      = ballotStatus.phase.unsafe()
    val highBallotOpt              = ballotStatus.highBallot.unsafe()
    val commitBallotOpt            = ballotStatus.commit.unsafe()
    if (phase != Ballot.Phase.Confirm) false
    else if (highBallotOpt.isEmpty || commitBallotOpt.isEmpty) false
    else if (!ballot.compatible(commitBallotOpt.get)) false
    else true
  }

  /** get current confirmed ballot
    */
  override def currentConfirmedBallot(slotIndex: SlotIndex): Stack[Ballot] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.commit.unsafe().getOrElse(Ballot.bottom)
  }

  /** get current nominating round
    */
  override def currentNominateRound(slotIndex: SlotIndex): Stack[Int] = Stack {
    val nominationStatus: NominationStatus = slotIndex
    nominationStatus.roundNumber.unsafe()
  }

  /** set nominate round to the next one
    */
  override def gotoNextNominateRound(slotIndex: SlotIndex): Stack[Unit] = Stack {
    val nominationStatus: NominationStatus = slotIndex
    nominationStatus.roundNumber := nominationStatus.roundNumber.unsafe() + 1
    ()
  }

  /** save new values to current accepted nominations
    */
  override def acceptNewNominations(slotIndex: SlotIndex, values: ValueSet): Stack[Unit] = Stack {
    val nominationStatus: NominationStatus = slotIndex
    nominationStatus.accepted := values
    ()
  }

  /** find latest candidate value
    */
  override def currentCandidateValue(slotIndex: SlotIndex): Stack[Option[Value]] =
    Stack {
      val nominationStatus: NominationStatus = slotIndex
      nominationStatus.latestCompositeCandidate.unsafe()
    }

  override def candidateValueUpdated(slotIndex: SlotIndex, composite: Value): Stack[Unit] = Stack {
    val nominationStatus: NominationStatus = slotIndex
    nominationStatus.latestCompositeCandidate := Some(composite)
    ()
  }

  /** check if a envelope can be emitted
    */
  override def canEmit[M <: Message](slotIndex: SlotIndex, envelope: Envelope[M]): Stack[Boolean] =
    Stack {
      envelope.statement.message match {
        case n: Nomination =>
          val nominationStatus: NominationStatus = slotIndex
          val lastEnv                            = nominationStatus.lastEnvelope
          lastEnv.isEmpty || lastEnv.unsafe().isEmpty || n.isNewerThan(
            lastEnv.unsafe().get.statement.message)
        case b: BallotMessage =>
          val ballotStatus: BallotStatus = slotIndex
          val canEmit                    = !ballotStatus.currentBallot.unsafe().isBottom
          val lastEnv                    = ballotStatus.latestGeneratedEnvelope.map(_.statement.message)
          canEmit && (lastEnv.isEmpty || isNewerBallotMessage(b, lastEnv.unsafe()))
      }
    }

  override def shouldProcess[M <: Message](slotIndex: SlotIndex,
                                           envelope: Envelope[M]): Stack[Boolean] =
    Stack { setting =>
      envelope.statement.message match {
        case _: BallotMessage =>
          val ballotStatus: BallotStatus = slotIndex
          val lastEnv: Var[Option[Envelope[BallotMessage]]] =
            ballotStatus.latestEnvelopes.map(_.get(setting.localNode))
          lastEnv.isEmpty || !lastEnv.unsafe().contains(envelope)
        case _ => false
      }
    }

  override def envelopeReadyToBeEmitted[M <: Message](slotIndex: SlotIndex,
                                                      envelope: Envelope[M]): Stack[Unit] = Stack {
    envelope.statement.message match {
      case _: BallotMessage =>
        val ballotStatus = slotIndex
        ballotStatus.latestGeneratedEnvelope := envelope.asInstanceOf[Envelope[BallotMessage]]
      case _ =>
    }

    ()
  }

  override def localNode(): Stack[NodeID] = Stack { setting =>
    setting.localNode
  }

  override def nominateEnvelope(slotIndex: SlotIndex): Stack[Option[Envelope[Message.Nomination]]] =
    Stack {
      val nominationStatus: NominationStatus = slotIndex
      nominationStatus.lastEnvelope.unsafe()
    }

  override def ballotEnvelope(
      slotIndex: SlotIndex): Stack[Option[Envelope[Message.BallotMessage]]] = Stack {
    val ballotStatus: BallotStatus = slotIndex
    ballotStatus.latestEmitEnvelope.unsafe()
  }

  implicit def getNominationStatus(slotIndex: SlotIndex): NominationStatus = {
    NominationStatus.getInstance(slotIndex)
  }

  implicit def getBallotStatus(slotIndex: SlotIndex): BallotStatus = {
    BallotStatus.getInstance(slotIndex)
  }

  private def updateCurrentIfNeed(slotIndex: SlotIndex, ballot: Ballot): Boolean = {
    val ballotStatus: BallotStatus = slotIndex
    val currentBallot              = ballotStatus.currentBallot.unsafe()
    if (currentBallot.isBottom || currentBallot.compare(ballot) < 0) {
      bumpToBallot(slotIndex, ballot, check = true)
    } else false
  }

  private def bumpToBallot(slotIndex: SlotIndex, ballot: Ballot, check: Boolean): Boolean = {
    val ballotStatus: BallotStatus = slotIndex
    val phase                      = ballotStatus.phase.unsafe()
    val currentBallot              = ballotStatus.currentBallot.unsafe()
    val phaseValid                 = phase != Ballot.Phase.Externalize
    val currentBallotValid =
      if (check) currentBallot.isBottom || ballot.compare(currentBallot) >= 0 else true

    if (currentBallot.isBottom || currentBallot.counter != ballot.counter) {
      ballotStatus.heardFromQuorum := false
    }

    ballotStatus.currentBallot := ballot

    val highBallotOpt = ballotStatus.highBallot.unsafe()
    if (highBallotOpt.nonEmpty && !ballot.compatible(highBallotOpt.get)) {
      ballotStatus.highBallot := None
    }
    phaseValid && currentBallotValid
  }

  private def checkInvariants(slotIndex: SlotIndex): Boolean = {
    val ballotStatus: BallotStatus = slotIndex
    val currentBallot              = ballotStatus.currentBallot.unsafe()
    val prepared                   = ballotStatus.prepared.unsafe()
    val preparedPrim               = ballotStatus.preparedPrime.unsafe()
    val commit                     = ballotStatus.commit.unsafe()
    val highBallot                 = ballotStatus.highBallot.unsafe()

    def currentBallotValid: Boolean = currentBallot.isBottom || currentBallot.counter != 0

    def prepareValid: Boolean = {
      prepared.isEmpty || preparedPrim.isEmpty ||
      preparedPrim.get.isLess(prepared.get) && preparedPrim.get.incompatible(prepared.get)
    }

    def highValid: Boolean = {
      highBallot.isEmpty ||
      !currentBallot.isBottom && highBallot.get.isLess(currentBallot) && highBallot.get
        .compatible(currentBallot)
    }

    def commitValid: Boolean = {
      commit.isEmpty ||
      (!currentBallot.isBottom && highBallot.nonEmpty && commit.get
        .isLess(highBallot.get) && commit.get.compatible(highBallot.get) && highBallot.get
        .isLess(currentBallot) && highBallot.get.compatible(currentBallot))
    }

    val phaseValid: Boolean = ballotStatus.phase.unsafe() match {
      case Ballot.Phase.Prepare     => true
      case Ballot.Phase.Confirm     => commit.nonEmpty
      case Ballot.Phase.Externalize => commit.nonEmpty && highBallot.nonEmpty
    }

    currentBallotValid && prepareValid && highValid && commitValid && phaseValid
  }

  def isNewerBallotMessage(current: BallotMessage, old: BallotMessage): Boolean = {
    val isDiff = current != old

    val isGrow = current match {
      case newPrepare: Message.Prepare =>
        old match {
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
        old match {
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
      case _: Message.Externalize =>
        old match {
          case _: Message.Externalize => false
          case _                      => true
        }
    }
    isDiff && isGrow
  }
}

object NodeStoreHandler {
  val instance = new NodeStoreHandler

  trait Implicits {
    implicit val scpNodeStoreHandler: NodeStoreHandler = instance
  }
}
