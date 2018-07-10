package fssi.interpreter.scp

import bigknife.scalap.ast.types.Value
import fssi.ast.domain.types.{Moment, Transaction}
import fssi.contract.States
import fssi.interpreter.jsonCodec._
import io.circe.syntax._

case class MomentValue(moments: Vector[Moment]) extends Value{
  override def compare(that: Value): Int = that match {
    case MomentValue(thatMoments) =>
      if (this.moments.isEmpty && thatMoments.isEmpty) 0
      else if (this.moments.isEmpty && thatMoments.nonEmpty) -1
      else if (this.moments.nonEmpty && thatMoments.isEmpty) 1
      else {
        val thisMax = moments.maxBy(_.timestamp)
        val thatMax = thatMoments.maxBy(_.timestamp)
        (thisMax.timestamp - thatMax.timestamp).toInt
      }
    case _ => -1

  }

  override def bytes: Array[Byte] = moments.asJson.noSpaces.getBytes("utf-8")

  override def toString(): String = moments.asJson.spaces2
}

object MomentValue {
  def apply(moments: Moment*): MomentValue = MomentValue(moments.toVector)
  def empty: MomentValue = MomentValue(Vector.empty)
}
