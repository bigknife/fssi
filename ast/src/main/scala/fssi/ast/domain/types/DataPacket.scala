package fssi.ast.domain.types

sealed trait DataPacket {
  def uuid: String
}

object DataPacket {
  case class CreateAccount(data: Account) extends DataPacket {
    override def uuid: String = s"CreateAccount(id=${data.id.value})"
  }
  case class SyncAccount(id: Account.ID) extends DataPacket {
    override def uuid: String = s"SyncAccount(id=${id.value})"
  }
  case class SubmitTransaction(transaction: Transaction) extends DataPacket {
    override def uuid: String = s"Transaction(sig=${transaction.signature.base64})"
  }
  case class TypedString(message: String, `type`: String) extends DataPacket {
    override def uuid: String =
      s"TypedString(${BytesValue(message.getBytes("utf-8") ++ `type`.getBytes("utf-8")).md5})"
  }


}
