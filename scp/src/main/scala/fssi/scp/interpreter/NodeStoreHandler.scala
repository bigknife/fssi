package fssi.scp
package interpreter

import bigknife.sop._
import fssi.scp.ast._
import fssi.scp.interpreter.store.{BallotStatus, NominationStatus, Var}
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
    nominationStatus.votes := newVotes; ()
  }

  /** the set of nodes which have voted(nominate x)
    */
  override def nodesVotedNomination(nodeId: NodeID,
                                    slotIndex: SlotIndex,
                                    value: Value): Stack[Set[NodeID]] = Stack {
    val nominationStatus: NominationStatus = (nodeId, slotIndex)
    nominationStatus.latestNominations
      .map { map =>
        map.keySet.filter(n => map(n).statement.message.voted.contains(value))
      }
      .unsafe()
  }

  /** the set of nodes which have accept(nominate x)
    */
  override def nodesAcceptedNomination(nodeId: NodeID,
                                       slotIndex: SlotIndex,
                                       value: Value): Stack[Set[NodeID]] = ???

  /** save a new value to current accepted nominations
    */
  override def acceptNewNomination(nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   value: Value): Stack[Unit] = ???

  /** save a new value to current candidated nominations
    */
  override def candidateNewNomination(nodeId: NodeID,
                                      slotIndex: SlotIndex,
                                      value: Value): Stack[Unit] = ???

  /** get current ballot
    */
  override def currentBallot(nodeId: NodeID, slotIndex: SlotIndex): Stack[Option[Ballot]] = ???

  /** given a ballot, get next ballot to try base on local state (z)
    * if there is a value stored in z, use <z, counter>, or use <attempt, counter>
    */
  override def nextBallotToTry(nodeId: NodeID,
                               slotIndex: SlotIndex,
                               attempt: Value,
                               counter: Int): Stack[Ballot] = ???

  /** update local state when a new ballot was bumped into
    * @see BallotProtocol.cpp#399
    */
  override def updateBallotStateWhenBumpNewBallot(nodeId: NodeID,
                                                  slotIndex: SlotIndex,
                                                  newB: Ballot): Stack[Boolean] = ???

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
  override def nodesAheadLocal(nodeId: NodeID, slotIndex: SlotIndex): Stack[Set[NodeID]] = ???

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
  override def currentBallotPhase(nodeId: NodeID, slotIndex: SlotIndex): Stack[Ballot.Phase] = ???

  /** get `c` in local state
    */
  override def currentCommitBallot(nodeId: NodeID, slotIndex: SlotIndex): Stack[Option[Ballot]] =
    ???

  /** get current message level
    * message level is used to control `attempBump` only bening invoked once when advancing ballot which
    * would cause recursive-invoking.
    */
  override def currentMessageLevel(nodeId: NodeID, slotIndex: SlotIndex): Stack[Int]      = ???
  override def currentMessageLevelUp(nodeId: NodeID, slotIndex: SlotIndex): Stack[Unit]   = ???
  override def currentMessageLevelDown(nodeId: NodeID, slotIndex: SlotIndex): Stack[Unit] = ???

  /** find all counters from received ballot message envelopes
    * @see BallotProtocol.cpp#1338
    */
  override def allCountersFromBallotEnvelopes(nodeId: NodeID,
                                              slotIndex: SlotIndex): Stack[CounterSet] = ???

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
    ???

  /** the set of nodes which have vote(prepare b)
    * @see BallotProtocol.cpp#839-866
    */
  override def nodesVotedPrepare(nodeId: NodeID,
                                 slotIndex: SlotIndex,
                                 ballot: Ballot): Stack[Set[NodeID]] = ???

  /** the set of nodes which have accepted(prepare b)
    * @see BallotProtocol.cpp#1521
    */
  override def nodesAcceptedPrepare(nodeId: NodeID,
                                    slotIndex: SlotIndex,
                                    ballot: Ballot): Stack[Set[NodeID]] = ???

  /** find all the commitable counters in recieved envelopes
    * @see BallotProtocol.cpp#1117
    */
  override def commitBoundaries(nodeId: NodeID,
                                slotIndex: SlotIndex,
                                ballot: Ballot): Stack[CounterSet] = ???

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
  override def currentNominateRound(nodeId: NodeID, slotIndex: SlotIndex): Stack[Int] = ???

  /** set nominate round to the next one
    */
  override def gotoNextNominateRound(nodeId: NodeID, slotIndex: SlotIndex): Stack[Unit] = ???

  /** save new values to current accepted nominations
    */
  override def acceptNewNominations(nodeId: NodeID,
                                    slotIndex: SlotIndex,
                                    values: ValueSet): Stack[Unit] = ???

  /** find latest candidate value
    */
  override def currentCandidateValue(nodeId: NodeID, slotIndex: SlotIndex): Stack[Option[Value]] =
    ???

  /** check if a envelope can be emitted
    */
  override def canEmit[M <: Message](nodeId: NodeID,
                                     slotIndex: SlotIndex,
                                     envelope: Envelope[M]): Stack[Boolean] = ???

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
