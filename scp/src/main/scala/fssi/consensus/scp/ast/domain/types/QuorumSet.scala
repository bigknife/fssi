package fssi.consensus.scp.ast.domain.types

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

  trait Op {
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
              } else 0
            }
            .getOrElse(0)
        }
      }

      def isQuorumSlice(nodeIDs: Set[Node.ID]): Boolean = {
        val thresholdLeft = qs.validators.foldLeft(qs.threshold) { (acc, n) =>
          if (nodeIDs.exists(_.bytes sameElements n.bytes)) acc - 1
          else acc
        }
        if (thresholdLeft <= 0) true
        else {
          val thresholdLeft1 = qs.innerSets.foldLeft(thresholdLeft) { (acc, n) =>
            if (n.isQuorumSlice(nodeIDs)) acc - 1
            else acc
          }
          if (thresholdLeft1 <= 0) true
          else false
        }
      }

      def isVBlocking(nodeIDs: Set[Node.ID]): Boolean = {
        if (qs.threshold <= 0) false
        else {
          val leftTillBlock = (qs.validators.size + qs.innerSets.size + 1) - qs.threshold
          val left0 = qs.validators.foldLeft(leftTillBlock) { (acc, n) =>
            if (nodeIDs.exists(_.bytes sameElements n.bytes)) acc - 1
            else acc
          }

          if (left0 <= 0) true
          else {
            val left1 = qs.innerSets.foldLeft(left0) { (acc, n) =>
              if (n.isVBlocking(nodeIDs)) acc - 1
              else acc
            }
            if (left1 <= 0) true
            else false
          }
        }
      }

      def forAll(f: Node.ID => Unit): Unit = {
        def _inner(qs0: QuorumSet,  f0: Node.ID => Unit): Unit = {
          qs0.validators.foreach(x => f0(Node.ID(x.bytes)))
          qs0.innerSets.foreach(x => _inner(x, f0))
        }
        // to ensure every node is invoked once
        val invoked: scala.collection.mutable.ListBuffer[Node.ID] = scala.collection.mutable.ListBuffer.empty
        _inner(qs, nodeID => {
          if (invoked.contains(nodeID)) ()
          else {
            f(nodeID)
            invoked.append(nodeID)
          }

        })
      }

      def isQuorum(envelopes: Map[Node.ID, Envelope],
                   quorumFunc: Statement => QuorumSet,
                   filter: Statement => Boolean): Boolean = {
        val filteredNodeIDs = envelopes.filter(x => filter(x._2.statement)).keys
        ???
      }
    }
  }

  object op extends Op

}
