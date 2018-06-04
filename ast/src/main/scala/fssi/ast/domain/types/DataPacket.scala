package fssi.ast.domain.types

sealed trait DataPacket {

}

object DataPacket {
  case class CreateAccount(data: Account) extends DataPacket
}
