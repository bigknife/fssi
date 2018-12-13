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
  val log = LoggerFactory.getLogger(getClass)
  trait Implicits {
    import fssi.base.implicits._
    import fssi.scp.types.implicits._

    implicit def statementToBytes[M <: Message](statement: Statement[M]): Array[Byte] = {
      val from = statement.from.asBytesValue.any
//      log.error(s"from hash: ------------${from.base64}")
      val slotIndex = statement.slotIndex.asBytesValue.any
//      log.error(s"slot index hash: ---------${slotIndex.base64}")
      val quorum = statement.quorumSet.asBytesValue.any
//      log.error(s"quorum hash: --------------${quorum.base64}")
      val message = statement.message.asBytesValue.any
//      log.error(s"message hash: --------------${message.base64}")
      val bytes = (from ++ slotIndex ++ quorum ++ message).bytes
      bytes
    }
  }
}
