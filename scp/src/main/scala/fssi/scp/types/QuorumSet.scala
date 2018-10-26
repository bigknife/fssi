package fssi.scp.types

sealed trait QuorumSet

object QuorumSet {
  /** stateless quorum set, include node's slices in every message
    */
  case class QuorumSlices(slices: Slices) extends QuorumSet

  /** slices reference
    */
  case class QuorumRef(ref: Array[Byte]) extends QuorumSet

  def slices(slices: Slices): QuorumSet = QuorumSlices(slices)
  def ref(r: Array[Byte]): QuorumSet = QuorumRef(r)

  /** Quorum Slices
    */
  sealed trait Slices {
    def threshold: Int

    final def nest(threshold: Int, validators: NodeID*): Slices = this match {
      case Slices.Flat(threshold1, validators1) =>
        Slices.nest(threshold1,
          validators1,
          Slices.flat(threshold, validators: _*).asInstanceOf[Slices.Flat])
      case Slices.Nest(threshold1, validators1, inners1) =>
        Slices.nest(threshold1,
          validators1,
          inners1 :+ Slices.flat(threshold, validators: _*).asInstanceOf[Slices.Flat]: _*)
    }

    final def allNodes: Set[NodeID] = this match {
      case Slices.Flat(_, validators) => validators.toSet
      case Slices.Nest(_, validators, inners) =>
        inners.foldLeft(validators.toSet) {(acc, n) =>
          acc ++ n.validators.toSet
        }
    }
  }

  object Slices {
    def flat(threshold: Int, validators: NodeID*): Slices =
      Flat(threshold, validators.toVector)

    def nest(threshold: Int, validators: Vector[NodeID], inners: Flat*): Slices =
      Nest(threshold, validators, inners.toVector)

    case class Flat(threshold: Int, validators: Vector[NodeID]) extends Slices {
      /*
      override def toString: String = {
        val nodes = validators.map(_.asBytesValue.bcBase58).mkString(",")
        s"{$threshold|$nodes}"
      }
      */
    }

    case class Nest(threshold: Int, validators: Vector[NodeID], inners: Vector[Flat]) extends Slices {
      /*
      override def toString: String = {
        val nodes = validators.map(_.asBytesValue.bcBase58).mkString(",")
        s"{$threshold|$nodes,${inners.map(_.toString).mkString(",")}}"
      }
      */
    }
  }
}
