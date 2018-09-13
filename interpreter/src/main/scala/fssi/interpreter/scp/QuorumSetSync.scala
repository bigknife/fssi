package fssi
package interpreter
package scp

import java.nio.ByteBuffer
import java.security.MessageDigest

import bigknife.scalap.ast.types.{Hash, NodeID, QuorumSet}
import utils._

/**
  * QuorumSet Synchronize Message
  */
case class QuorumSetSync(
    version: Long,
    registeredQuorumSets: Map[NodeID, QuorumSet],
    hash: Hash
) {
  def isSane: Boolean =
    QuorumSetSync.hash(version, registeredQuorumSets) == hash

  def merge(qss: QuorumSetSync): QuorumSetSync = {
    if (qss.hash == this.hash) this
    else {
      val sets = qss.registeredQuorumSets ++ this.registeredQuorumSets

      val newVersion =
        if (qss.registeredQuorumSets == this.registeredQuorumSets) Math.max(version, qss.version)
        else Math.max(version, qss.version) + 1
      val newHash = QuorumSetSync.hash(newVersion, sets)
      QuorumSetSync(newVersion, sets, newHash)

    }
  }

}

object QuorumSetSync {
  def toBytes(quorumSet: QuorumSet): Array[Byte] = {
    quorumSet match {
      case QuorumSet.Simple(threshold, validators) =>
        // threshold bytes
        val bb = ByteBuffer.allocate(4)
        bb.putInt(threshold)
        validators.toVector.sortBy(_.asHex()).foldLeft(bb.array()) { _ ++ _.bytes }

      case QuorumSet.Nest(threshold, validators, innerSets) =>
        val simpleBytes = toBytes(QuorumSet.Simple(threshold, validators))
        innerSets.map(toBytes).toVector.sortBy(x => BytesValue(x).md5).foldLeft(simpleBytes) {
          _ ++ _
        }
    }
  }
  def hash(version: Long, registeredQuorumSets: Map[NodeID, QuorumSet]): Hash = {
    val bb = ByteBuffer.allocate(8)
    bb.putLong(version)
    val bytes = registeredQuorumSets.toVector
      .sortBy(_._1.asHex())
      .map(x => x._1.bytes ++ toBytes(x._2))
      .foldLeft(bb.array()) { _ ++ _ }

    val md5 = MessageDigest.getInstance("md5").digest(bytes)

    Hash(md5)
  }
}
