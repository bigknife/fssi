package fssi.scp.types

case class Statement(
  from: NodeID,
  timestamp: Timestamp,
  quorumSet: QuorumSet,
  message: Message
)
