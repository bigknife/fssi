package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleVoteNominationsProgram[F[_]] extends SCP[F] with EmitProgram[F] {
  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._

  // when received vote(nominate x) message, we should try to advance this x to accepted
  def handleVoteNominations(nodeId: NodeID,
                            slotIndex: SlotIndex,
                            previousValue: Value,
                            statement: Statement[Message.VoteNominations]): SP[F, Boolean] = {

    // remove values which have been voted or accepted
    lazy val completelyNewValues: SP[F, ValueSet] =
      statement.message.values.foldLeft(ValueSet.empty.pureSP[F]) { (acc, n) =>
        for {
          pre <- acc
          a   <- valueVotedOrAccepted(nodeId, slotIndex, n)
        } yield if (a) pre else pre + n
      }

    // is nominating? terminate when nominating stopped
    lazy val nominatingStopped: SP[F, Boolean] = isNominatingStopped(nodeId, slotIndex)

    // narrowDown
    lazy val narrowed = for {
      values <- completelyNewValues
      xs     <- narrowDownVotes(nodeId, slotIndex, values, previousValue)
    } yield xs

    // application-level valid narrowed
    lazy val validNarrowed: SP[F, ValueSet] = for {
      values <- narrowed
      valid <- values.foldLeft(ValueSet.empty.pureSP[F]) { (acc, n) =>
        for {
          pre <- acc
          a   <- validateValue(n)
        } yield if (a) pre + n else pre
      }
    } yield valid

    // federated accept to promote votes to accepted
    lazy val promoteVotesToAccepted: SP[F, Boolean] = for {
      unAccepted <- unAcceptedNominations(nodeId, slotIndex)
      promoted <- unAccepted.foldLeft(false.pureSP[F]) { (acc, n) =>
        for {
          pre           <- acc
          nodesVoted    <- nodesVotedNomination(nodeId, slotIndex, n)
          nodesAccepted <- nodesAcceptedNomination(nodeId, slotIndex, n)
          federatedAccepted <- ifM(isVBlocking(nodeId, nodesAccepted), right = true)(
            isQuorum(nodeId, nodesVoted ++ nodesAccepted))
          _ <- ifThen(federatedAccepted)(acceptNewNominations(nodeId, slotIndex, ValueSet(n)))
        } yield pre || federatedAccepted
      }

    } yield promoted

    ifM(nominatingStopped, right = false) {
      for {
        newVotes <- validNarrowed
        voted <- ifM(newVotes.isEmpty, right = false)(
          voteNewNominations(nodeId, slotIndex, newVotes).map(_ => true))
        accepted <- promoteVotesToAccepted
        voteMessage <- ifM(!voted, Option.empty[Message.VoteNominations]) {
          for {
            msg <- createVoteNominationMessage(nodeId, slotIndex)
          } yield Option(msg)
        }
        acceptMessage <- ifM(!accepted, Option.empty[Message.AcceptNominations]) {
          for {
            msg <- createAcceptNominationMessage(nodeId, slotIndex)
          } yield Option(msg)
        }
        bunchedMessage = Message.bunchOption(voteMessage, acceptMessage)
        _ <- ifThen(bunchedMessage.nonEmpty) {
          emit(nodeId, slotIndex, previousValue, bunchedMessage)
        }
      } yield true
    }
  }
}
