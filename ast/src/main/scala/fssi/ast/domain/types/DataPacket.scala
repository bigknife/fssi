package fssi.ast.domain.types

sealed trait DataPacket {

}

object DataPacket {
  case class CreateAccount(data: Account) extends DataPacket
  case class SyncAccount(id: Account.ID) extends DataPacket
  case class SubmitTransaction(transaction: Transaction) extends DataPacket
  case class TypedString(message: String, `type`: String) extends DataPacket

  def scpEnvelope(message: String): DataPacket = TypedString(message, "scp.envelope")
  object ScpEnvelope {
    def unapply(arg: DataPacket): Option[String] = arg match {
      case TypedString(message, "scp.envelope") => Some(message)
      case _ => None
    }
  }
}
