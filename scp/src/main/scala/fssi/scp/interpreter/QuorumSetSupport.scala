package fssi.scp
package interpreter

import fssi.scp.interpreter.store._
import fssi.scp.types._

trait QuorumSetSupport extends LogSupport{
  import QuorumSet._
  import QuorumSetSupport._

  def unsafeGetSlices(nodeId: NodeID): QuorumSet.Slices =
    slicesCache.map(_.get(nodeId)).unsafe.get

  /** delete a node (validators) from slices.
    * Definition (delete).
    * If ⟨𝐕, 𝐐⟩ is an FBAS and 𝐵 ⊆ 𝐕 is a set of nodes,
    * then to delete 𝐵 from ⟨𝐕, 𝐐⟩, written ⟨𝐕, 𝐐⟩𝐵 ,
    * means to compute the modified FBAS ⟨𝐕 ⧵ 𝐵, 𝐐𝐵 ⟩ where 𝐐𝐵 (𝑣) = { 𝑞 ⧵ 𝐵 ∣ 𝑞 ∈ 𝐐(𝑣) }.
    */
  def deleteNodeFromSlices(slices: Slices, nodeId: NodeID): Slices = {
    log.debug(s"deleting $nodeId from $slices")
    slices match {
      case Slices.Flat(threshold, validators) =>
        // acc is (remained validators, remained threshold)
        val (remainedValidators, remainedThreshold) =
          validators.foldLeft((Vector.empty[NodeID], threshold)) { (acc, n) =>
            if (n === nodeId) (acc._1, acc._2 - 1)
            else (acc._1 :+ n, acc._2)
          }
        Slices.flat(remainedThreshold, remainedValidators: _*)
      case Slices.Nest(threshold, validators, inners) =>
        val (remainedValidators, remainedThreshold) =
          validators.foldLeft((Vector.empty[NodeID], threshold)) { (acc, n) =>
            if (n === nodeId) (acc._1, acc._2 - 1)
            else (acc._1 :+ n, acc._2)
          }
        val remainedInners = inners.map { x =>
          val (x_remainedValidators, x_remainedThreshold) =
            x.validators.foldLeft((Vector.empty[NodeID], x.threshold)) { (acc, n) =>
              if (n === nodeId) (acc._1, acc._2 - 1)
              else (acc._1 :+ n, acc._2)
            }
          Slices.Flat(x_remainedThreshold, x_remainedValidators)
        }
        Slices.nest(remainedThreshold, remainedValidators, remainedInners:_*)
    }
  }

  /** simplify slices:
    * simplifies singleton inner set into outerset:
    *   { t: n, v: { ... }, { t: 1, X }, ... } -> { t: n, v: { ..., X }, .... }
    * simplifies singleton inner sets:
    *   { t:1, { innerSet } } into innerSet
    */
  def simplifySlices(slices: Slices): Slices = {
    slices match {
      case x: Slices.Flat => x
      case Slices.Nest(threshold, validators, inner) =>
        // simplifies singleton inner sets
        if (threshold == 1 && validators.isEmpty && inner.size == 1) inner.head
        else {
          // simplifies singleton inner set into outer sets
          val (v, i) = inner.foldLeft((validators, Vector.empty[Slices.Flat])) {(acc, n) =>
            if (n.validators.size == 1 && n.threshold == 1) (acc._1 :+ n.validators.head, acc._2)
            else (acc._1, acc._2 :+ n)
          }
          Slices.nest(threshold, v, i: _*)
        }
    }
  }
}

object QuorumSetSupport {
  private val slicesCache: Var[Map[NodeID, QuorumSet.Slices]] = Var(Map.empty)
}
