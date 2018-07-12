package fssi.interpreter.scp

import bigknife.scalap.ast.types.Value
import fssi.ast.domain.types.{BytesValue, Moment, Transaction}
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

  lazy val bytes: Array[Byte] = {
    val x = moments.foldLeft(Array.emptyByteArray) {(acc, n) => acc ++ n.bytes}
    val h1 = java.security.MessageDigest.getInstance("md5").digest(x)
    println(BytesValue(h1).hex)
    x
  }

  //override def bytes: Array[Byte] = _bytes


  override def toString(): String = moments.asJson.spaces2
}

object MomentValue {
  def apply(moments: Moment*): MomentValue = MomentValue(moments.toVector)
  def empty: MomentValue = MomentValue(Vector.empty)
}
