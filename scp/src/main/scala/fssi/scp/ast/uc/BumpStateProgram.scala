package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait BumpStateProgram[F[_]] extends SCP[F] with EmitProgram[F] {
  import model.applicationService._
  import model.nodeService._
  import model.nodeStore._

  /** a bridge function, nomination process can bump to ballot process
    * @param force force to set local state
    */
  private[uc] def bumpState(nodeId: NodeID,
                            slotIndex: SlotIndex,
                            previousValue: Value,
                            compositeValue: Value,
                            force: Boolean): SP[F, Boolean] = {
    // if forced to bump, check current ballot, if exists, ignore.
    for {
      b <- currentBallot(nodeId, slotIndex)
      bumped <- ifM(!force && b.isDefined, false) {
        val n = b.map(_.counter + 1).getOrElse(1)
        bumpState(nodeId, slotIndex, previousValue, compositeValue, n)
      }
    } yield bumped
  }

  /** bump to a ballot
    */
  private[uc] def bumpState(nodeId: NodeID,
                            slotIndex: SlotIndex,
                            previousValue: Value,
                            value: Value,
                            counter: Int): SP[F, Boolean] = {

    for {
      newB    <- nextBallotToTry(nodeId, slotIndex, value, counter)
      updated <- updateBallotState(nodeId, slotIndex, newB)
      _ <- ifThen(updated) {
        for {
          msg <- createBallotMessage(nodeId, slotIndex)
          _   <- emit(nodeId, slotIndex, previousValue, msg)
          _   <- checkHeardFromQuorum(nodeId, slotIndex, previousValue)
        } yield ()
      }
    } yield updated
  }

  /** check all receviced nodes  ahead of local state to see if they can be totally construct a
    * quorum.
    * whey they can be contruct a quorum to local node whech also a quorum for it's element node,
    * we try to force to bump current ballot with latest composite value after a while
    */
  private[uc] def checkHeardFromQuorum(nodeId: NodeID,
                                       slotIndex: SlotIndex,
                                       previousValue: Value): SP[F, Unit] = {
    def totallyQuoromNodes(xs: Set[NodeID]): SP[F, Set[NodeID]] = {

      def deleteM(xs: Set[NodeID]): SP[F, Set[NodeID]] =
        xs.foldLeft(xs.pureSP[F]) { (acc, n) =>
          for {
            pre <- acc
            q   <- isQuorum(n, xs)
          } yield if (q) pre else (pre - n)
        }

      def _loop(xs: Set[NodeID]): SP[F, Set[NodeID]] =
        for {
          remain <- deleteM(xs)
          result <- ifM(remain.size == xs.size, xs)(_loop(remain))
        } yield result

      _loop(xs)
    }

    def startBallotProtocolTimer(): SP[F, Unit] =
      for {
        b       <- currentBallot(nodeId, slotIndex)
        timeout <- computeTimeout(b.map(_.counter).getOrElse(0))
        _ <- delayExecuteProgram(BALLOT_TIMER,
                                 abandonBallot(nodeId, slotIndex, previousValue, 0),
                                 timeout)
      } yield ()

    def stopBallotProtocolTimer(): SP[F, Unit] = stopDelayTimer(BALLOT_TIMER)

    for {
      phase       <- currentBallotPhase(nodeId, slotIndex)
      aheads      <- nodesAheadLocal(nodeId, slotIndex)
      quorumNodes <- totallyQuoromNodes(aheads)
      nowHeard    <- isQuorum(nodeId, quorumNodes)
      everHeard   <- isHeardFromQuorum(nodeId, slotIndex)
      _           <- heardFromQuorum(nodeId, slotIndex, nowHeard)
      _ <- ifThen((nowHeard && !everHeard) && phase != Ballot.Phase.Externalize)(
        startBallotProtocolTimer())
      _ <- ifThen((nowHeard && !everHeard) && phase == Ballot.Phase.Externalize)(
        stopBallotProtocolTimer())
      _ <- ifThen(!nowHeard)(stopBallotProtocolTimer())
    } yield ()
  }

  private[uc] def abandonBallot(nodeId: NodeID,
                                slotIndex: SlotIndex,
                                previousValue: Value,
                                counter: Int): SP[F, Boolean] =
    for {
      candidate <- currentCandidateValue(nodeId, slotIndex)
      value <- ifM(candidate.isDefined, candidate)(
        currentBallot(nodeId, slotIndex).map(_.map(_.value)))
      r <- ifM(value.isEmpty, false)(
        ifM(counter == 0, bumpState(nodeId, slotIndex, previousValue, value.get, true))(
          bumpState(nodeId, slotIndex, previousValue, value.get, counter)))
    } yield true

}
