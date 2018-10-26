package fssi.scp.types

case class Statement[M <: Message](
  from: NodeID,
  slotIndex: SlotIndex,
  timestamp: Timestamp,
  quorumSet: QuorumSet,
  message: M
) {
  def to[N <: Message]: Statement[N] = this.asInstanceOf[Statement[N]]

  def withMessage[N <: Message](n: N): Statement[N] = Statement(from, slotIndex, timestamp, quorumSet, n)
}
