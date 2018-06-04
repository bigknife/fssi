package fssi.ast.domain.types

case class Account(
    id: Account.ID,
    privateKeyData: BytesValue,
    publicKeyData: BytesValue,
    iv: BytesValue,
    balance: Token
) {
  override def toString: String = s"Account(${id.value})"
}

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
    object Created extends Status {
      override def toString: String = "Created"
    }

    /** disseminated to at least one warrior node */
    object Disseminated extends Status {
      override def toString: String = "Disseminated"
    }

    /** synced from other nodes */
    object Synced extends Status {
      override def toString: String = "Synced"
    }

    def apply(s: String): Status = s match {
      case x if x equals "Synced"       => Synced
      case x if x equals "Disseminated" => Synced
      case _                            => Created
    }
  }

}
