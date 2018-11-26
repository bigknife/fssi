package fssi.scp
package interpreter

import fssi.base.implicits._
import fssi.scp.ast._
import fssi.scp.interpreter.store._
import fssi.scp.types._
import fssi.scp.types.implicits._
import fssi.utils._

class NodeServiceHandler
    extends NodeService.Handler[Stack]
    with LogSupport
    with SafetyGuard
    with CryptoSupport
    with QuorumSetSupport {
  import log._

  override def cacheNodeQuorumSet(nodeId: NodeID, quorumSet: QuorumSet): Stack[Unit] = Stack {
    quorumSet match {
      case QuorumSet.QuorumSlices(slices) => addNodeSlices(nodeId, slices)
      case QuorumSet.QuorumRef(_)         =>
    }
  }

  /** compute next round timeout (in ms)
    *
    * @see SCPDriver.cpp#79
    */
  override def computeTimeout(round: Int): Stack[Long] = Stack { setting =>
//    if (round > setting.maxTimeoutSeconds) setting.maxTimeoutSeconds * 1000L
//    else round * 1000L
    10 * 1000L
  }

  /** check if in-nominating and no any candidate produced
    */
  override def canNominateNewValue(slotIndex: SlotIndex, timeout: Boolean): Stack[Boolean] = Stack {
    setting =>
      assertSlotIndex(setting.localNode, slotIndex)

      val nominationStatus  = NominationStatus.getInstance(slotIndex)
      val nominationStarted = nominationStatus.nominationStarted.getOrElse(false)
      if (timeout && !nominationStarted) {
        debug(s"triggered by nomination timer, but nomination had been stopped")
        false
      } else {
        debug(s"triggered by application request")
        true
      }

  }

  /** check if nominating is stopped
    */
  override def isNominatingStopped(slotIndex: SlotIndex): Stack[Boolean] = Stack { setting =>
    assertSlotIndex(setting.localNode, slotIndex)

    val nominationStatus = NominationStatus.getInstance(slotIndex)
    !nominationStatus.nominationStarted.unsafe()
  }

  override def filtrateVotes(values: ValueSet): Stack[ValueSet] =
    Stack(values.filterNot {
      case _: FakeValue => true
      case _            => false
    })

  /** compute a value's hash
    *
    * @see SCPDriver.cpp#66 notice: added hash_K
    */
  override def hashValue(slotIndex: SlotIndex,
                         previousValue: Value,
                         round: Int,
                         value: Value): Stack[Long] = Stack { setting =>
    if (Option(setting.applicationCallback).exists(_.isHashFuncProvided)) {
      setting.applicationCallback.hashValue(slotIndex, previousValue, round, value)
    } else {
      computeHashValue(
        slotIndex.value.toByteArray ++ previousValue.rawBytes ++ NodeServiceHandler.hash_K ++ BigInt(
          round).toByteArray ++ value.rawBytes).toLong
    }
  }

  /** start nomination process
    */
  override def startNominating(slotIndex: SlotIndex): Stack[Unit] = Stack { setting =>
    assertSlotIndex(setting.localNode, slotIndex)
    NominationStatus.getInstance(slotIndex).nominationStarted := true
    ()
  }

  /** stop nomination process
    */
  override def stopNominating(slotIndex: SlotIndex): Stack[Unit] = Stack { setting =>
    assertSlotIndex(setting.localNode, slotIndex)
    NominationStatus.getInstance(slotIndex).nominationStarted := false
    ()
  }

  /** do some rate-limits stuff to narrow down the nominating votes
    *
    * @see NominationProtocl.cpp#476-506
    */
  override def updateAndGetNominateLeaders(slotIndex: SlotIndex,
                                           previousValue: Value): Stack[Set[NodeID]] = Stack {
    setting =>
      assertSlotIndex(setting.localNode, slotIndex)
      val nominationStatus = NominationStatus.getInstance(slotIndex)
      val round            = nominationStatus.roundNumber.unsafe()

      // clean roundLeaders and initialized with local nodeId
      nominationStatus.roundLeaders := Set(setting.localNode)

      // normalized quorumSet(slices) of nodeId
      val myQSet         = unsafeGetSlices(setting.localNode)
      val normalizedQSet = simplifySlices(deleteNodeFromSlices(myQSet, setting.localNode))

      // initialize top priority as localId
      val topPriority =
        computeNodePriority(setting.localNode,
                            slotIndex,
                            isLocal = true,
                            previousValue,
                            round,
                            normalizedQSet,
                            setting)
      log.debug(s"topPriority: $topPriority")

      // find top priority nodes
      val (leaders, newTopPriority) =
        normalizedQSet.allNodes.foldLeft((Set(setting.localNode), topPriority)) { (acc, n) =>
          val (topNodes, currentTopPriority) = acc
          val nPriority =
            computeNodePriority(n,
                                slotIndex,
                                isLocal = false,
                                previousValue,
                                round,
                                normalizedQSet,
                                setting)
          if (nPriority == currentTopPriority && nPriority > 0) (topNodes + n, currentTopPriority)
          else if (nPriority > currentTopPriority) (Set(n), nPriority)
          else acc
        }
      log.debug(s"found ${leaders.size} leaders at new top priority: $newTopPriority")
      nominationStatus.roundLeaders := leaders
      log.debug(s"current leaders : $leaders on $slotIndex for previous value: $previousValue")
      leaders
  }

  /** create nomination message based on local state
    */
  override def createNominationMessage(slotIndex: SlotIndex): Stack[Message.Nomination] = Stack {
    setting =>
      assertSlotIndex(setting.localNode, slotIndex)

      val nominationStatus = NominationStatus.getInstance(slotIndex)
      Message.Nomination(
        voted = nominationStatus.votes.unsafe(),
        accepted = nominationStatus.accepted.unsafe()
      )
  }

  /** create ballot message based on local state
    */
  override def createBallotMessage(slotIndex: SlotIndex): Stack[Message.BallotMessage] = Stack {
    setting =>
      assertSlotIndex(setting.localNode, slotIndex)

      val ballotStatus = BallotStatus.getInstance(slotIndex)
      val phase        = ballotStatus.phase.unsafe()

      phase match {
        case Ballot.Phase.Prepare =>
          Message.Prepare(
            b = ballotStatus.currentBallot.unsafe(),
            p = ballotStatus.prepared.unsafe(),
            `p'` = ballotStatus.preparedPrime.unsafe(),
            `c.n` = ballotStatus.commit.unsafe().map(_.counter).getOrElse(0),
            `h.n` = ballotStatus.highBallot.unsafe().map(_.counter).getOrElse(0)
          )
        case Ballot.Phase.Confirm =>
          Message.Confirm(
            b = ballotStatus.currentBallot.unsafe(),
            `p.n` = ballotStatus.prepared.unsafe().map(_.counter).getOrElse(0),
            `c.n` = ballotStatus.commit.unsafe().map(_.counter).getOrElse(0),
            `h.n` = ballotStatus.highBallot.unsafe().map(_.counter).getOrElse(0)
          )
        case Ballot.Phase.Externalize =>
          Message.Externalize(
            x = ballotStatus.commit.unsafe().map(_.value).get,
            `c.n` = ballotStatus.commit.unsafe().map(_.counter).get,
            `h.n` = ballotStatus.highBallot.unsafe().map(_.counter).getOrElse(0)
          )
      }
  }

  /** make a envelope for a message
    */
  override def putInEnvelope[M <: Message](slotIndex: SlotIndex, message: M): Stack[Envelope[M]] =
    Stack { setting =>
      val statement = Statement(
        from = setting.localNode,
        slotIndex = slotIndex,
        timestamp = Timestamp(System.currentTimeMillis()),
        quorumSet = setting.quorumSet,
        message = message
      )

      val signature =
        Signature(crypto.makeSignature(fixedStatementBytes(statement), setting.privateKey))

      Envelope(statement, signature)
    }

  /** verify the signature of the envelope
    */
  override def isSignatureVerified[M <: Message](envelope: Envelope[M]): Stack[Boolean] = Stack {
    val signature = envelope.signature
    val fromNode  = envelope.statement.from
    val publicKey = crypto.rebuildECPublicKey(fromNode.value, cryptoUtil.SECP256K1)
    val source    = fixedStatementBytes(envelope.statement)
    val verified  = crypto.verifySignature(signature.value, source, publicKey)
    verified
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
  override def isLocalQuorum(nodes: Set[NodeID]): Stack[Boolean] = Stack { setting =>
    isQuorumImpl(setting.localNode, nodes)
  }

  override def isQuorum(nodeID: NodeID, nodes: Set[NodeID]): Stack[Boolean] = Stack {
    isQuorumImpl(nodeID, nodes)
  }

  private def isQuorumImpl(nodeID: NodeID, nodes: Set[NodeID]): Boolean = {
    import QuorumSet._
    unsafeGetSlices(nodeID) match {
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
  override def isLocalVBlocking(nodes: Set[NodeID]): Stack[Boolean] = Stack { setting =>
    isVBlockingImpl(setting.localNode, nodes)
  }

  override def isVBlocking(nodeID: NodeID, nodes: Set[NodeID]): Stack[Boolean] = Stack {
    isVBlockingImpl(nodeID, nodes)
  }

  private def isVBlockingImpl(nodeID: NodeID, nodes: Set[NodeID]): Boolean = {
    import QuorumSet._
    unsafeGetSlices(nodeID) match {
      case x if x.threshold == 0 => false
      case Slices.Flat(threshold, validators) =>
        val blockingNum = validators.size - threshold + 1
        // if count of the intersection between validators and nodes is `ge` blockingNum, then it's a v-blocking set
        nodes.count(validators.contains) >= blockingNum
      case Slices.Nest(threshold, validators, inners) =>
        val blockingNum = validators.size + inners.size - threshold + 1
        // inners is a Vector of Flat. if any inner is not covered, then it's not v-blocking set
        // then check the outter, inner's v-blocking count + outter validators' v-blocking, if that number is ge blockingNum
        //      totally, it's a v-blocking set
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
      case Message.Prepare(b, _, _, _, _)                        => ValueSet(b.value)
      case Message.Confirm(b, _, _, _)                           => ValueSet(b.value)
      case Message.Externalize(x, _, _)                          => ValueSet(x)
    }
  }

  /** check a ballot can be used as a prepared candidate based on local p , p' and phase
    *
    * @see BallotProcotol.cpp#807-834
    */
  override def canBallotBePrepared(slotIndex: SlotIndex, ballot: Ballot): Stack[Boolean] = Stack {
    setting =>
      assertSlotIndex(setting.localNode, slotIndex)
      val ballotStatus = BallotStatus.getInstance(slotIndex)
      val phase        = ballotStatus.phase.unsafe()

      val b1 = if (phase == Ballot.Phase.Confirm) {
        val p: Ballot = ballotStatus.prepared.unsafe().getOrElse(Ballot.bottom)
        p <= ballot && p.compatible(ballot)
      } else true

      val pPrime = ballotStatus.preparedPrime.unsafe()
      val b2     = !(pPrime.isDefined && ballot <= pPrime.get)

      val p  = ballotStatus.prepared.unsafe()
      val b3 = !(p.isDefined && (ballot <= p.get && ballot.compatible(p.get)))

      b1 && b2 && b3
  }

  /** check a ballot can be potentially raise h, be confirmed prepared, to a commit
    *
    * @see BallotProtocol.cpp#937-938
    */
  override def canBallotBeHighestCommitPotentially(slotIndex: SlotIndex,
                                                   ballot: Ballot): Stack[Boolean] = Stack {
    val ballotStatus = BallotStatus.getInstance(slotIndex)
    val h            = ballotStatus.highBallot.unsafe()
    h.isEmpty || ballot > h.get
  }

  /** check a ballot can be potentially raise h, be confirmed prepared, to a commit
    *
    * @see BallotProtocol.cpp#970-973, 975-978 b should be compatible with newH
    */
  override def canBallotBeLowestCommitPotentially(slotIndex: SlotIndex,
                                                  b: Ballot,
                                                  newH: Ballot): Stack[Boolean] = Stack {
    val ballotStatus  = BallotStatus.getInstance(slotIndex)
    val currentBallot = ballotStatus.currentBallot.unsafe()
    (b >= currentBallot) && b.compatible(newH)
  }

  /** check if it's necessary to set `c` based on a new `h`
    *
    * @see BallotProtocol.cpp#961
    */
  override def needSetLowestCommitBallotUnderHigh(slotIndex: SlotIndex,
                                                  high: Ballot): Stack[Boolean] = Stack {
    val ballotStatus = BallotStatus.getInstance(slotIndex)
    val c            = ballotStatus.commit.unsafe()
    val p            = ballotStatus.prepared.unsafe()
    val pPrime       = ballotStatus.preparedPrime.unsafe()

    c.isEmpty &&
    (p.isEmpty || !(high <= p.get && high.incompatible(p.get))) &&
    (pPrime.isEmpty || !(high <= pPrime.get && high.incompatible(pPrime.get)))
  }

  override def broadcastTimeout(): Stack[Long] = Stack(setting => setting.broadcastTimeout)

  override def blockFakeValue(slotIndex: SlotIndex): Stack[FakeValue] = Stack(FakeValue(slotIndex))

  //////
  /** MUST be deterministic
    */
  /** check the message to see if it's sane
    *
    * @see BallotProcotol.cpp#247
    */
  private def isMessageSane[M <: Message](nodeId: NodeID, statement: Statement[M]): Boolean = {
    statement.message match {
      case Message.Nomination(votes, accepted) =>
        (votes.size + accepted.size) > 0
      case Message.Prepare(b, p, pPrime, nC, nH) =>
        // self is allowed to have b = 0 (as long as it never gets emitted)
        (if (statement.from === nodeId) b.counter >= 0 else b.counter > 0) &&
          (p.isEmpty || pPrime.isEmpty || areBallotsLessAndIncompatible(pPrime.get, p.get)) &&
          (nH == 0 || (p.isDefined && nH <= p.get.counter)) &&
          (nC == 0 || (nH != 0 && b.counter >= nH && nH >= nC))
      case Message.Confirm(b, nPrepared, nCommit, nH) =>
        b.counter > 0 && nH <= b.counter && nCommit <= nH
      case Message.Externalize(x, nC, nH) =>
        nC > 0 && nH >= nC
    }
  }

  private def areBallotsLessAndIncompatible(b1: Ballot, b2: Ballot): Boolean =
    b1 <= b2 && b1.incompatible(b2)

  private def fixedStatementBytes[M <: Message](statement: Statement[M]): Array[Byte] =
    statement.asBytesValue.bytes

  /** compute a node weight in some slices
    *
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
                                  round: Int,
                                  setting: Setting): Long =
    if (Option(setting.applicationCallback).exists(_.isHashFuncProvided)) {
      setting.applicationCallback.hashNodeForPriority(nodeId, slotIndex, previousValue, round)
    } else {
      val bytes = slotIndex.value.toByteArray ++ previousValue.rawBytes ++ NodeServiceHandler.hash_P ++ BigInt(
        round).toByteArray ++ nodeId.value
      computeHashValue(bytes).toLong
    }

  private def hashNodeForNeighbour(nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   previousValue: Value,
                                   round: Int,
                                   setting: Setting): Long =
    if (Option(setting.applicationCallback).exists(_.isHashFuncProvided)) {
      setting.applicationCallback.hashNodeForNeighbour(nodeId, slotIndex, previousValue, round)
    } else {
      val bytes = slotIndex.value.toByteArray ++ previousValue.rawBytes ++ NodeServiceHandler.hash_P ++ BigInt(
        round).toByteArray ++ nodeId.value
      computeHashValue(bytes).toLong
    }

  private def computeNodePriority(nodeId: NodeID,
                                  slotIndex: SlotIndex,
                                  isLocal: Boolean,
                                  previousValue: Value,
                                  round: Int,
                                  slices: QuorumSet.Slices,
                                  setting: Setting): Long = {
    val w = computeNodeWeight(nodeId, isLocal, slices)
    // if it's a neighbour, then compute the priority, or else 0(the smallest, because hash is uint64)
    if (hashNodeForNeighbour(nodeId, slotIndex, previousValue, round, setting) < w)
      hashNodeForPriority(nodeId, slotIndex, previousValue, round, setting)
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
