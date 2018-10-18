package fssi.scp.types

case class Statement[M <: Message](
  from: NodeID,
  timestamp: Timestamp,
  quorumSet: QuorumSet,
  message: M
) {
  def to[N <: Message]: Statement[N] = this.asInstanceOf[Statement[N]]
}
