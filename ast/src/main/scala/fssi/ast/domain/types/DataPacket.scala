package fssi.ast.domain.types

sealed trait DataPacket {

}

object DataPacket {
  case class CreateAccount(data: Account) extends DataPacket
  case class SyncAccount(id: Account.ID) extends DataPacket
  case class SubmitTransaction(account: Account, transaction: Transaction) extends DataPacket
  case class TypedString(message: String, `type`: String) extends DataPacket
}
