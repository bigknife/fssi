package fssi.ast.domain.types

case class Account(
    id: Account.ID,
    prv: KeyPair.Priv,
    pub: KeyPair.Publ,
    iv: BytesValue,
    balance: Token
)

object Account {
  case class ID(value: String)
}
