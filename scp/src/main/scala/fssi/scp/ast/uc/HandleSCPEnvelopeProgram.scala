package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleSCPEnvelopeProgram[F[_]]
    extends SCP[F]
    with HandleVoteNominationsProgram[F]
    with HandleAcceptNominationsProgram[F]
    with HandleVotePrepareProgram[F]
    with HandleAcceptPrepareProgram[F]
    with HandleVoteCommitProgram[F]
    with HandleAcceptCommitProgram[F]
    with HandleExternalizeProgram[F] {
  import model._

  /** process message envelope from peer nodes
    */
  def handleSCPEnvelope[M <: Message](nodeId: NodeID,
                                      slotIndex: SlotIndex,
                                      envelope: Envelope[M],
                                      previousValue: Value): SP[F, Boolean] = {
    import nodeService._
    import nodeStore._

    def checkEnvelope: SP[F, Boolean] =
      for {
        verified <- verifySignature(envelope)
        valid    <- checkStatementValidity(envelope.statement)
        isNewer <- isStatementNewer(nodeId, slotIndex, envelope.statement)
      } yield verified && valid && isNewer
    def envelopeCheckFailed: SP[F, Boolean] = checkEnvelope.map(!_)

    ifM(envelopeCheckFailed, right = false) {
      for {
        _ <- saveLatestStatement(nodeId, slotIndex, envelope.statement)
        r <- handleStatement(nodeId, slotIndex, previousValue, envelope.statement)
      } yield r
    }
  }

  private def handleStatement[M <: Message](nodeId: NodeID,
                                            slotIndex: SlotIndex,
                                            previousValue: Value,
                                            statement: Statement[M]): SP[F, Boolean] =
    statement.message match {
      case _: Message.VoteNominations =>
        handleVoteNominations(nodeId,
                              slotIndex,
                              previousValue,
                              statement.to[Message.VoteNominations])
      case _: Message.AcceptNominations =>
        handleAcceptNominations(nodeId,
                                slotIndex,
                                previousValue,
                                statement.to[Message.AcceptNominations])
      case _: Message.VotePrepare =>
        handleVotePrepare(nodeId, slotIndex, previousValue, statement.to[Message.VotePrepare])
      case _: Message.AcceptPrepare =>
        handleAcceptPrepare(nodeId, slotIndex, previousValue, statement.to[Message.AcceptPrepare])
      case _: Message.VoteCommit =>
        handleVoteCommit(nodeId, slotIndex, previousValue, statement.to[Message.VoteCommit])
      case _: Message.AcceptCommit =>
        handleAcceptCommit(nodeId, slotIndex, previousValue, statement.to[Message.AcceptCommit])
      case _: Message.Externalize =>
        handleExternalize(nodeId, slotIndex, previousValue, statement.to[Message.Externalize])
      case Message.Bunches(xs) =>
        xs.foldLeft(false.pureSP[F]) { (acc, n) =>
          for {
            pre <- acc
            r   <- handleStatement(nodeId, slotIndex, previousValue, statement.copy(message = n))
          } yield pre && r
        }
    }

}
