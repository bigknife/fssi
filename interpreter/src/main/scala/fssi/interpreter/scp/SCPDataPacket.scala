package fssi.interpreter.scp

import fssi.ast.domain.types._
import DataPacket._
import bigknife.scalap.ast.types.{Envelope, Message}
import io.circe.syntax._
import io.circe.parser._
import fssi.interpreter.jsonCodec._
import org.slf4j.LoggerFactory

trait SCPDataPacket {
  private val log               = LoggerFactory.getLogger(getClass)
  val MessageType_SyncQuorumSet = "scp.discover.quorumset"
  val MessageType_SCPEnvelope   = "scp.envelope"

  val QuorumSetJsonFile = "quorumsets.json"

  def scpEnvelope[M <: Message](envelope: Envelope[M]): DataPacket = {
    val message = envelope.asJson.noSpaces
    TypedString(message, MessageType_SCPEnvelope)
  }
  object ScpEnvelope {
    def unapply(arg: DataPacket): Option[Envelope[Message]] = arg match {
      case TypedString(message, MessageType_SCPEnvelope) =>
        val ret = for {
          json <- parse(message)
          qss  <- json.as[Envelope[Message]]
        } yield qss

        ret match {
          case Left(t) =>
            log.error("parse Envelope[Message] failed", t)
            None
          case Right(x) => Some(x)
        }

      case _                                             => None
    }
  }

  def syncQuorumSets(qss: QuorumSetSync): DataPacket = {
    val message = qss.asJson.noSpaces
    TypedString(message, MessageType_SyncQuorumSet)
  }

  object QuorumSetSyncMsg {
    def unapply(arg: DataPacket): Option[QuorumSetSync] = arg match {
      case TypedString(message, MessageType_SyncQuorumSet) =>
        val ret = for {
          json <- parse(message)
          qss  <- json.as[QuorumSetSync]
        } yield qss

        ret match {
          case Left(t) =>
            log.error("parse QuorumSetSyncMsg failed", t)
            None
          case Right(x) => Some(x)
        }

      case _ => None
    }
  }
}
object SCPDataPacket extends SCPDataPacket
