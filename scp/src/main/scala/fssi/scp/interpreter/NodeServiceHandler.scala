package fssi.scp
package interpreter

import bigknife.sop._

import fssi.scp.ast._
import fssi.scp.types._
import fssi.scp.interpreter.store._
import fssi.utils._

class NodeServiceHandler
    extends NodeService.Handler[Stack]
    with LogSupport
    with SafetyGuard
    with CryptoSupport
    with QuorumSetSupport {
  import log._

  /** compute next round timeout (in ms)
    * @see SCPDriver.cpp#79
    */
  override def computeTimeout(round: Int): Stack[Long] = Stack { setting =>
    val roundNumber = if (round <= 0) 1 else round + 1
    if (roundNumber > setting.maxTimeoutSeconds) setting.maxTimeoutSeconds * 1000L
    else roundNumber * 1000L
  }

  /** check if in-nominating and no any candidate produced
    */
  override def canNominateNewValue(nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   timeout: Boolean): Stack[Boolean] = Stack {
    assertSlotIndex(nodeId, slotIndex)

    val nominationStatus  = NominationStatus.getInstance(nodeId, slotIndex)
    val nominationStarted = nominationStatus.nominationStarted.getOrElse(false)
    if (timeout && !nominationStarted) {
      info(s"triggered by nomination timer, but nomination had been stopped")
      false
    } else {
      info(s"triggered by application request")
      true
    }

  }

  /** check if nominating is stopped
    */
  override def isNominatingStopped(nodeId: NodeID, slotIndex: SlotIndex): Stack[Boolean] = Stack {
    assertSlotIndex(nodeId, slotIndex)

    val nominationStatus = NominationStatus.getInstance(nodeId, slotIndex)
    nominationStatus.nominationStarted.unsafe
  }

  /** compute a value's hash
    * @see SCPDriver.cpp#66 notice: added hash_K
    */
  override def hashValue(slotIndex: SlotIndex,
                         previousValue: Value,
                         round: Int,
                         value: Value): Stack[Long] = Stack {
    computeHashValue(
      slotIndex.value.toByteArray ++ previousValue.rawBytes ++ NodeServiceHandler.hash_K ++ BigInt(
        round).toByteArray ++ value.rawBytes).toLong
  }

  /** stop nomination process
    */
  override def stopNominating(nodeId: NodeID, slotIndex: SlotIndex): Stack[Unit] = Stack {
    assertSlotIndex(nodeId, slotIndex)
    NominationStatus.getInstance(nodeId, slotIndex).nominationStarted := false
    ()
  }

  /** do some rate-limits stuff to narrow down the nominating votes
    * @see NominationProtocl.cpp#476-506
    */
  override def updateAndGetNominateLeaders(nodeId: NodeID,
                                           slotIndex: SlotIndex,
                                           previousValue: Value): Stack[Set[NodeID]] = Stack {

    assertSlotIndex(nodeId, slotIndex)
    val nominationStatus = NominationStatus.getInstance(nodeId, slotIndex)
    val round            = nominationStatus.roundNumber.unsafe

    // clean roundLeaders and initialized with local nodeId
    nominationStatus.roundLeaders := Set(nodeId)

    // normalized quorumSet(slices) of nodeId
    val myQSet         = unsafeGetSlices(nodeId)
    val normalizedQSet = simplifySlices(deleteNodeFromSlices(myQSet, nodeId))

    // initialize top priority as localId
    val topPriority =
      computeNodePriority(nodeId, slotIndex, isLocal = true, previousValue, round, normalizedQSet)
    log.debug(s"topPriority: $topPriority")

    // find top priority nodes
    val (leaders, newTopPriority) =
      normalizedQSet.allNodes.foldLeft((Set.empty[NodeID], topPriority)) { (acc, n) =>
        val (topNodes, currentTopPriority) = acc
        val nPriority =
          computeNodePriority(n, slotIndex, isLocal = false, previousValue, round, normalizedQSet)
        if (nPriority == currentTopPriority && nPriority > 0) (topNodes + n, currentTopPriority)
        else if (nPriority > currentTopPriority) (Set(n), nPriority)
        else acc
      }
    log.debug(s"found ${leaders.size} leaders at new top priority: $newTopPriority")
    nominationStatus.roundLeaders := leaders
    leaders
  }

  /** create nomination message based on local state
    */
  override def createNominationMessage(nodeId: NodeID,
                                       slotIndex: SlotIndex): Stack[Message.Nomination] = Stack {
    assertSlotIndex(nodeId, slotIndex)

    val nominationStatus = NominationStatus.getInstance(nodeId, slotIndex)
    Message.Nomination(
      voted = nominationStatus.votes.unsafe,
      accepted = nominationStatus.accepted.unsafe
    )
  }

  /** create ballot message based on local state
    */
  override def createBallotMessage(nodeId: NodeID,
                                   slotIndex: SlotIndex): Stack[Message.BallotMessage] = Stack {
    assertSlotIndex(nodeId, slotIndex)

    val ballotStatus = BallotStatus.getInstance(nodeId, slotIndex)
    val phase        = ballotStatus.phase.unsafe

    phase match {
      case Ballot.Phase.Prepare =>
        Message.Prepare(
          b = ballotStatus.currentBallot.unsafe,
          p = ballotStatus.prepared.unsafe,
          `p'` = ballotStatus.preparedPrime.unsafe,
          `c.n` = ballotStatus.commit.unsafe.map(_.counter).getOrElse(0),
          `h.n` = ballotStatus.highBallot.unsafe.map(_.counter).getOrElse(0)
        )
      case Ballot.Phase.Confirm =>
        Message.Confirm(
          b = ballotStatus.currentBallot.unsafe,
          `p.n` = ballotStatus.prepared.unsafe.map(_.counter).getOrElse(0),
          `c.n` = ballotStatus.commit.unsafe.map(_.counter).getOrElse(0),
          `h.n` = ballotStatus.highBallot.unsafe.map(_.counter).getOrElse(0)
        )
      case Ballot.Phase.Externalize =>
        Message.Externalize(
          x = ballotStatus.commit.unsafe.map(_.value).get,
          `c.n` = ballotStatus.commit.unsafe.map(_.counter).get,
          `h.n` = ballotStatus.highBallot.unsafe.map(_.counter).getOrElse(0)
        )
    }
  }

  /** make a envelope for a message
    */
  override def putInEnvelope[M <: Message](nodeId: NodeID,
                                           slotIndex: SlotIndex,
                                           message: M): Stack[Envelope[M]] = Stack { setting =>
    val statement = Statement(
      from = nodeId,
      slotIndex = slotIndex,
      timestamp = Timestamp(System.currentTimeMillis()),
      quorumSet = setting.quorumSet,
      message = message
    )

    val signature =
      Signature(crypto.makeSignature(fixedStatementBytes(statement), setting.privateKey))

    val env = Envelope(statement, signature)

    message match {
      case x: Message.BallotMessage =>
        val nominationStatus = BallotStatus.getInstance(nodeId, slotIndex)
        nominationStatus.latestGeneratedEnvelope := env.to[Message.BallotMessage]
      case _ =>
    }

    env
  }

  /** verify the signature of the envelope
    */
  override def isSignatureVerified[M <: Message](envelope: Envelope[M]): Stack[Boolean] = Stack {
    val signature = envelope.signature
    val fromNode  = envelope.statement.from
    val publicKey = crypto.rebuildECPublicKey(fromNode.value, "secp256k1")
    val source    = fixedStatementBytes(envelope.statement)
    crypto.verifySignature(signature.value, source, publicKey)
  }

  /** check the statement to see if it is illegal
    */
  override def isStatementValid[M <: Message](nodeId: NodeID,
                                              slotIndex: SlotIndex,
                                              statement: Statement[M]): Stack[Boolean] = Stack {
    (slotIndex == statement.slotIndex) &&
    (System.currentTimeMillis - statement.timestamp.value).abs <= 30 * 60 * 1000 && (
      statement.quorumSet match {
        case x: QuorumSet.QuorumRef => false
        case QuorumSet.QuorumSlices(QuorumSet.Slices.Flat(threshold, validators)) =>
          threshold <= validators.size && threshold > 0 && validators.size == validators.toSet.size
        case QuorumSet.QuorumSlices(QuorumSet.Slices.Nest(threshold, validators, inners)) =>
          threshold <= (validators.size + inners.size) && threshold > 0 && validators.size == validators.toSet.size && !inners
            .exists { x =>
              x.threshold <= x.validators.size && x.threshold > 0 && x.validators.size == x.validators.toSet.size
            }

      }
    ) && isMessageSane(nodeId, statement)
    //todo check if quorum set is sane
  }

  /** check a node set to see if they can construct a quorum for a node (configured quorum slices)
    */
  override def isQuorum(nodeId: NodeID, nodes: Set[NodeID]): Stack[Boolean] = Stack {
    import QuorumSet._
    unsafeGetSlices(nodeId) match {
      case Slices.Flat(threshold, validators) =>
        nodes.count(validators.contains) >= threshold
      case Slices.Nest(threshold, validators, inners) =>
        val innerCount = inners.count { f =>
          nodes.count(f.validators.contains) >= f.threshold
        }
        val outterCount = nodes.count(validators.contains)
        (innerCount + outterCount) >= threshold
    }
  }

  /** check a node set to see if they can construct a vblocking set for a node (configured quorum slices)
    */
  override def isVBlocking(nodeId: NodeID, nodes: Set[NodeID]): Stack[Boolean] = Stack {
    import QuorumSet._
    unsafeGetSlices(nodeId) match {
      case x if x.threshold == 0 => false
      case Slices.Flat(threshold, validators) =>
        val blockingNum = validators.size - threshold + 1
        // if count of the intersection between validators and nodes is `ge` blockingNum, then it's a vblocking set
        nodes.count(validators.contains) >= blockingNum
      case Slices.Nest(threshold, validators, inners) =>
        val blockingNum = validators.size + inners.size - threshold + 1
        // inners is a Vector of Flat. if any inner is not covered, then it's not vblocking set
        // then check the outter, inner's vblocking count + outter validators' vblocking, if that number is ge blockingNum
        //      totally, it's a vblocking set
        val innerBlockingCount = inners.count { f =>
          val fBlockingNum = f.validators.size - f.threshold + 1
          nodes.count(f.validators.contains) >= fBlockingNum
        }
        val outterBlockingCount = nodes.count(validators.contains)
        (innerBlockingCount + outterBlockingCount) >= blockingNum
    }
  }

  /** get values from a ballot message
    */
  override def valuesFromBallotMessage(msg: Message.BallotMessage): Stack[ValueSet] = Stack {
    msg match {
      case Message.Prepare(b, Some(p), _, _, _) if b.counter > 0 => ValueSet(b.value, p.value)
      case Message.Prepare(_, Some(p), _, _, _)                  => ValueSet(p.value)
      case Message.Confirm(b, _, _, _)                           => ValueSet(b.value)
      case Message.Externalize(x, _, _)                          => ValueSet(x)
    }
  }

  /** check a ballot can be used as a prepared candidate based on local p , p' and phase
    * @see BallotProcotol.cpp#807-834
    */
  override def canBallotBePrepared(nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   ballot: Ballot): Stack[Boolean] = Stack {
    assertSlotIndex(nodeId, slotIndex)
    val ballotStatus = BallotStatus.getInstance(nodeId, slotIndex)
    val phase        = ballotStatus.phase.unsafe

    val b1 = if (phase == Ballot.Phase.Confirm) {
      val p: Ballot = ballotStatus.prepared.unsafe.getOrElse(Ballot.bottom)
      (p <= ballot && p.compatible(ballot))
    } else true

    val pPrime = ballotStatus.preparedPrime.unsafe
    val b2     = !(pPrime.isDefined && ballot <= pPrime.get)

    val p  = ballotStatus.prepared.unsafe
    val b3 = !(p.isDefined && (ballot <= p.get && ballot.compatible(p.get)))

    b1 && b2 && b3
  }

  /** check a ballot can be potentially raise h, be confirmed prepared, to a commit
    * @see BallotProtocol.cpp#937-938
    */
  override def canBallotBeHighestCommitPotentially(nodeId: NodeID,
                                                   slotIndex: SlotIndex,
                                                   ballot: Ballot): Stack[Boolean] = Stack {
    val ballotStatus = BallotStatus.getInstance(nodeId, slotIndex)
    val h            = ballotStatus.highBallot.unsafe
    h.isEmpty || ballot > h.get
  }

  /** check a ballot can be potentially raise h, be confirmed prepared, to a commit
    * @see BallotProtocol.cpp#970-973, 975-978 b should be compatible with newH
    */
  override def canBallotBeLowestCommitPotentially(nodeId: NodeID,
                                                  slotIndex: SlotIndex,
                                                  b: Ballot,
                                                  newH: Ballot): Stack[Boolean] = Stack {
    val ballotStatus  = BallotStatus.getInstance(nodeId, slotIndex)
    val currentBallot = ballotStatus.currentBallot.unsafe
    (b >= currentBallot) && (b.compatible(newH))
  }

  /** check if it's necessary to set `c` based on a new `h`
    * @see BallotProtocol.cpp#961
    */
  override def needSetLowestCommitBallotUnderHigh(nodeId: NodeID,
                                                  slotIndex: SlotIndex,
                                                  high: Ballot): Stack[Boolean] = Stack {
    val ballotStatus = BallotStatus.getInstance(nodeId, slotIndex)
    val c            = ballotStatus.commit.unsafe
    val p            = ballotStatus.prepared.unsafe
    val pPrime       = ballotStatus.preparedPrime.unsafe

    c.isDefined &&
    (p.isEmpty || !(high <= p.get && high.incompatible(p.get))) &&
    (pPrime.isEmpty || !(high <= pPrime.get && high.incompatible(pPrime.get)))
  }

  //////
  /** MUST be deterministic
    */
  /** check the message to see if it's sane
    * @see BallotProcotol.cpp#247
    */
  private def isMessageSane[M <: Message](nodeId: NodeID, statement: Statement[M]): Boolean = {
    statement.message match {
      case Message.Nomination(votes, accepted) =>
        (votes.size + accepted.size) > 0
      case Message.Prepare(b, p, pPrime, nC, nH) =>
        // self is allowed to have b = 0 (as long as it never gets emitted)
        (if (statement.from === nodeId) b.counter >= 0 else b.counter > 0) &&
          (p.isEmpty || pPrime.isEmpty || areBallotsLessAndIncompatable(pPrime.get, p.get)) &&
          (nH == 0 || (p.isDefined && nH <= p.get.counter)) &&
          (nC == 0 || (nH != 0 && b.counter >= nH && nH >= nC))
      case Message.Confirm(b, nPrepared, nCommit, nH) =>
        b.counter > 0 && nH <= b.counter && nCommit <= nH
      case Message.Externalize(x, nC, nH) =>
        nC > 0 && nH >= nC
    }
  }

  private def areBallotsLessAndIncompatable(b1: Ballot, b2: Ballot): Boolean =
    b1 <= b2 && b1.incompatible(b2)

  private def fixedStatementBytes[M <: Message](statement: Statement[M]): Array[Byte] = ???

  /** compute a node weight in some slices
    * @param isLocal if nodeId is local node id, true, or else flase
    */
  private def computeNodeWeight(nodeId: NodeID,
                                isLocal: Boolean,
                                slices: QuorumSet.Slices): Long = {
    if (isLocal) Long.MaxValue
    else {
      import QuorumSet.Slices._
      def compute(a: BigInt, b: BigInt, c: BigInt): Long = {
        val bi = (a * b + c - 1) / c
        bi.toLong
      }

      slices match {
        case Flat(threshold, validators) if validators.contains(nodeId) =>
          // (a * b + c - 1) / c
          val a = BigInt(Long.MaxValue)
          val b = BigInt(threshold)
          val c = BigInt(validators.size)
          compute(a, b, c)

        case Nest(threshold, validators, inners) if validators.contains(nodeId) =>
          // (a * b + c - 1) / c
          val a = BigInt(Long.MaxValue)
          val b = BigInt(threshold)
          val c = BigInt(validators.size + inners.size)
          compute(a, b, c)

        case Nest(threshold, validators, inners) =>
          inners
            .find(_.validators.contains(nodeId))
            .map { s =>
              val a = BigInt(Long.MaxValue)
              val b = BigInt(s.threshold)
              val c = BigInt(s.validators.size)

              // the node's weight in inners
              val leafW = compute(a, b, c)
              // let the leafW as the `a`, recompute in total slices
              compute(a = leafW, b = BigInt(threshold), c = BigInt(validators.size + inners.size))
            }
            .getOrElse(0L)

        case _ => 0L
      }
    }
  }

  private def hashNodeForPriority(nodeId: NodeID,
                                  slotIndex: SlotIndex,
                                  previousValue: Value,
                                  round: Int): Long = {

    val bytes = slotIndex.value.toByteArray ++ previousValue.rawBytes ++ NodeServiceHandler.hash_P ++ BigInt(
      round).toByteArray ++ nodeId.value
    computeHashValue(bytes).toLong
  }

  private def hashNodeForNeighbour(nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   previousValue: Value,
                                   round: Int): Long = {

    val bytes = slotIndex.value.toByteArray ++ previousValue.rawBytes ++ NodeServiceHandler.hash_P ++ BigInt(
      round).toByteArray ++ nodeId.value
    computeHashValue(bytes).toLong
  }

  private def computeNodePriority(nodeId: NodeID,
                                  slotIndex: SlotIndex,
                                  isLocal: Boolean,
                                  previousValue: Value,
                                  round: Int,
                                  slices: QuorumSet.Slices): Long = {
    val w = computeNodeWeight(nodeId, isLocal, slices)
    // if it's a neighbour, then compute the priority, or else 0(the smallest, because hash is uint64)
    if (hashNodeForNeighbour(nodeId, slotIndex, previousValue, round) < w)
      hashNodeForPriority(nodeId, slotIndex, previousValue, round)
    else 0
  }

}

object NodeServiceHandler {
  val instance = new NodeServiceHandler

  private val hash_N = BigInt(1).toByteArray
  private val hash_P = BigInt(2).toByteArray
  private val hash_K = BigInt(3).toByteArray

  trait Implicits {
    implicit val scpNodeServiceHandler: NodeServiceHandler = instance
  }
}
