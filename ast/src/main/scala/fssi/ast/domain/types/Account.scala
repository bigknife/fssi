package fssi.ast.domain.types

case class Account(
    id: Account.ID,
    privateKeyData: BytesValue,
    publicKeyData: BytesValue,
    iv: BytesValue,
    balance: Token
)

object Account {
  case class ID(value: String)

  case class Snapshot(
      timestamp: Long,
      account: Account
  )
}
