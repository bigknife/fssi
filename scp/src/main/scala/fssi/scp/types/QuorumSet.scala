package fssi.scp.types
import fssi.scp.interpreter.LogSupport
import org.slf4j.LoggerFactory

sealed trait QuorumSet

object QuorumSet {

  /** stateless quorum set, include node's slices in every message
    */
  case class QuorumSlices(slices: Slices) extends QuorumSet

  /** slices reference
    */
  case class QuorumRef(ref: Array[Byte]) extends QuorumSet

  def slices(slices: Slices): QuorumSet = QuorumSlices(slices)
  def ref(r: Array[Byte]): QuorumSet    = QuorumRef(r)

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
        inners.foldLeft(validators.toSet) { (acc, n) =>
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

    case class Nest(threshold: Int, validators: Vector[NodeID], inners: Vector[Flat])
        extends Slices {
      /*
      override def toString: String = {
        val nodes = validators.map(_.asBytesValue.bcBase58).mkString(",")
        s"{$threshold|$nodes,${inners.map(_.toString).mkString(",")}}"
      }
     */
    }

    trait Implicits extends LogSupport {
      import fssi.base.implicits._
      import fssi.scp.types.implicits._
      implicit def flatToBytes(flat: Flat): Array[Byte] =
        flat.threshold.asBytesValue.bytes ++ flat.validators.toArray.asBytesValue.bytes

      implicit def nestToBytes(nest: Nest): Array[Byte] = {
        import fssi.utils._
        val threshold  = nest.threshold.asBytesValue.any
        val validators = nest.validators.toArray.asBytesValue.any
        val inners     = nest.inners.toArray.asBytesValue.any

        log.error("=============================================")
        log.error(s"      hash threshold: ${crypto.sha3(threshold.bytes).asBytesValue.bcBase58}")
        log.error(s"      hash validators: ${crypto.sha3(validators.bytes).asBytesValue.bcBase58}")
        log.error(s"      hash inners: ${crypto.sha3(inners.bytes).asBytesValue.bcBase58}")
        log.error(s"nest : $nest")
        log.error("=============================================")
        nest.threshold.asBytesValue.bytes ++ nest.validators.toArray.asBytesValue.bytes ++ nest.inners.toArray.asBytesValue.bytes
      }

      implicit def slicesToBytes(slices: Slices): Array[Byte] = slices match {
        case f: Slices.Flat => f.asBytesValue.bytes
        case n: Slices.Nest => n.asBytesValue.bytes
      }
    }
  }

  trait Implicits extends Slices.Implicits {
    import fssi.base.implicits._
    implicit def quorumSetToBytes(quorumSet: QuorumSet): Array[Byte] = quorumSet match {
      case QuorumSet.QuorumSlices(slices) => slices.asBytesValue.bytes
      case QuorumSet.QuorumRef(ref)       => ref
    }
  }
}
