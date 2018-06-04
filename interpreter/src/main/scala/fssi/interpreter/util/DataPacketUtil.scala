package fssi.interpreter.util

import fssi.ast.domain.types.DataPacket
import fssi.ast.domain.types.DataPacket.CreateAccount
import io.scalecube.transport.Message

/** DataPacket transformer */
trait DataPacketUtil {
  def toMessage[A <: DataPacket](a: A): Message = a match {
    case x: CreateAccount =>
      Message.builder().data(x.data).build()
  }
}

object DataPacketUtil extends DataPacketUtil
