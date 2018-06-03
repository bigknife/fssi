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
      account: Account,
      status: Snapshot.Status
  )

  object Snapshot {
    sealed trait Status

    /** created locally, have not been disseminated to warrior nodes*/
    object Created extends Status

    /** disseminated to at least one warrior node */
    object Disseminated extends Status

    /** synced from other nodes */
    object Synced extends Status

    def apply(s: String): Status = s match {
      case x if x equals "Synced"       => Synced
      case x if x equals "Disseminated" => Synced
      case _                            => Created
    }
  }

}
