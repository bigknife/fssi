package fssi.consensus.scp.types

import scala.math.BigDecimal.RoundingMode

sealed trait QuorumSet {
  def threshold: Int
  def validators: Vector[PublicKey]
  def innerSets: Vector[QuorumSet]
}

object QuorumSet {
  protected case class SimpleQuorumSet(
      threshold: Int = 0,
      validators: Vector[PublicKey] = Vector.empty
  ) extends QuorumSet {
    def innerSets: Vector[QuorumSet] = Vector.empty
  }

  protected case class TwoLayerQuorumSet(
      threshold: Int = 0,
      validators: Vector[PublicKey] = Vector.empty,
      innerSets: Vector[QuorumSet] = Vector.empty
  ) extends QuorumSet

  def singleton(nodeID: Node.ID): QuorumSet = SimpleQuorumSet(1, Vector(PublicKey(nodeID.bytes)))
  def simple(threshold: Int, validators: Vector[PublicKey]): QuorumSet =
    SimpleQuorumSet(threshold, validators)
  def twoLayers(threshold: Int,
                validators: Vector[PublicKey],
                quorumSets: Vector[QuorumSet]): QuorumSet =
    TwoLayerQuorumSet(threshold, validators, quorumSets.filter(_.innerSets.isEmpty))

  trait Syntax {
    implicit final class QuorumSetOp(qs: QuorumSet) {

      /** calculate a node's weight in this quorum set.
        *
        * @param nodeID node to be measured
        * @return a long number.
        */
      def nodeWeight(nodeID: Node.ID): Long = {
        val n: BigDecimal = BigDecimal(qs.threshold)
        val d: BigDecimal = BigDecimal(qs.validators.size + qs.innerSets.size)

        if (qs.validators.exists(_.bytes sameElements nodeID.bytes)) {
          // long.max * n / d
          (BigDecimal(Long.MaxValue) * n / d).setScale(0, RoundingMode.UP).toLong
        } else {
          // calculate node weight from inner sets
          val inner = qs.innerSets.find(_.validators.exists(_.bytes sameElements nodeID.bytes))
          inner
            .map { qs0 =>
              val leafW = qs0.nodeWeight(nodeID)
              if (leafW != 0) {
                (BigDecimal(leafW) * n / d).setScale(0, RoundingMode.UP).toLong
              }else 0
            }
            .getOrElse(0)
        }
      }

      /*
      def isQuorumSlice(nodeIDs: Set[Node.ID]): Boolean = {
        val thresholdLeft =  qs.validators.fold(qs.threshold) {(acc, n) =>
          if (nodeIDs.exists(_.bytes sameElements n.bytes)) acc - 1
          else acc
        }
        if (thresholdLeft <= 0) true
        else {
          qs.innerSets
        }
      }*/

    }
  }

  object syntax extends Syntax

}
