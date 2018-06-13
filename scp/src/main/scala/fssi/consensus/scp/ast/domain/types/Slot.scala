package fssi.consensus.scp.ast.domain.types

case class Slot(
    index: Long,
    nodeID: Node.ID,
    votes: Set[Value],
    accepted: Set[Value],
    candidates: Set[Value],
    roundLeaders: Set[Node.ID],
    lastEnvelope: Option[Envelope]
) {
  def accept(value: Value): Slot = copy(accepted = accepted + value)
  def vote(value: Value): Slot = copy(votes = votes + value)
  def candidate(value: Value): Slot = copy(candidates = candidates + value)
}

object Slot {
  sealed trait Change
  object Change {
    case object Identical extends Change {
      override def toString: String = "Identical"
    }
    case object Modified extends Change {
      override def toString: String = "Modified"
    }
    case object NewCandidate extends Change {
      override def toString: String = "NewCandidate"
    }
  }

  def noChange: Change = Change.Identical
  def modified: Change = Change.Modified
  def newCandidate: Change = Change.NewCandidate
}
