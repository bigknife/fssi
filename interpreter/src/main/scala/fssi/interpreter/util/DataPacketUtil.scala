package fssi.interpreter.util

import fssi.ast.domain.types.DataPacket
import fssi.ast.domain.types.DataPacket.CreateAccount
import io.scalecube.transport.Message

/** DataPacket transformer */
trait DataPacketUtil {
  // if it is not necessary, keep it simple.
  def toMessage[A <: DataPacket](a: A): Message = {
    val builder = Message.builder()
    builder
      .header("uuid", a.uuid)
      .header("timestamp", s"${System.currentTimeMillis()}")
      .data(a)
      .build()
  }
  /*
    a match {
    case x: CreateAccount =>
      Message.builder().data(x).build()
  }*/
}

object DataPacketUtil extends DataPacketUtil
