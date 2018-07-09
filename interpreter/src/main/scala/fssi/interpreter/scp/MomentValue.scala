package fssi.interpreter.scp

import bigknife.scalap.ast.types.Value
import fssi.ast.domain.types.Moment
import fssi.interpreter.jsonCodec._
import io.circe.syntax._

case class MomentValue(moment: Moment) extends Value{
  override def compare(that: Value): Int = that match {
    case MomentValue(thatMoment) => (moment.timestamp - thatMoment.timestamp).toInt
  }

  override def bytes: Array[Byte] = moment.asJson.noSpaces.getBytes("utf-8")

  override def toString(): String = moment.asJson.spaces2
}
