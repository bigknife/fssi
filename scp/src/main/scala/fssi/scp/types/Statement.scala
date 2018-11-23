package fssi.scp.types
import org.slf4j.LoggerFactory

case class Statement[M <: Message](
    from: NodeID,
    slotIndex: SlotIndex,
    timestamp: Timestamp,
    quorumSet: QuorumSet,
    message: M
) {
  def to[N <: Message]: Statement[N] = this.asInstanceOf[Statement[N]]

  def withMessage[N <: Message](n: N): Statement[N] =
    Statement(from, slotIndex, timestamp, quorumSet, n)
}

object Statement {
  private val log = LoggerFactory.getLogger(getClass)
  trait Implicits {
    import fssi.base.implicits._
    import fssi.scp.types.implicits._
    import fssi.utils.crypto

    implicit def statementToBytes[M <: Message](statement: Statement[M]): Array[Byte] = {
      val from      = statement.from.asBytesValue.any
      val slotIndex = statement.slotIndex.asBytesValue.any
      val timestamp = statement.timestamp.asBytesValue.any
      val quorum    = statement.quorumSet.asBytesValue.any
      val message   = statement.message.asBytesValue.any

      log.error("=============================================")
      log.error(s"from ${from.bcBase58}")
      log.error(s"      hash from: ${crypto.sha3(from.bytes).asBytesValue.bcBase58}")
      log.error(s"      hash slotIndex: ${crypto.sha3(slotIndex.bytes).asBytesValue.bcBase58}")
      log.error(s"      hash timestamp: ${crypto.sha3(timestamp.bytes).asBytesValue.bcBase58}")
      log.error(s"      hash quorum: ${crypto.sha3(quorum.bytes).asBytesValue.bcBase58}")
      log.error(s"      hash messdage: ${crypto.sha3(message.bytes).asBytesValue.bcBase58}")
      log.error("=============================================")

      (from ++ slotIndex ++ timestamp ++ quorum ++ message).bytes
    }
  }
}
