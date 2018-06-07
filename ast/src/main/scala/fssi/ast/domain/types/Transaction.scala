package fssi.ast.domain.types

/** transaction
  *   all activities happened on chain can be modeled as a transaction.
  */
sealed trait Transaction {
  def id: Transaction.ID
  def sender: Account.ID
  def signature: Signature
  def status: Transaction.Status
  def toBeVerified: BytesValue
}

object Transaction {
  // token transferred between two account
  case class Transfer(
      id: Transaction.ID,
      from: Account.ID,
      to: Account.ID,
      amount: Token,
      signature: Signature,
      status: Transaction.Status
  ) extends Transaction {
    val sender: Account.ID = from

    override def toBeVerified: BytesValue = {
      val buf = new StringBuilder
      buf.append(id.value)
      buf.append(from.value)
      buf.append(to.value)
      buf.append(amount.toString)
      BytesValue(buf.toString())
    }
  }

  // publish a contract
  case class PublishContract(
      id: Transaction.ID,
      owner: Account.ID,
      contract: Contract,
      signature: Signature,
      status: Transaction.Status
  ) extends Transaction {
    val sender: Account.ID = owner
    override def toBeVerified: BytesValue = {
      val buf = new StringBuilder
      buf.append(id.value)
      buf.append(owner.value)
      buf.append(contract.codeSign.base64)
      BytesValue(buf.toString())
    }
  }

  // invoke a contract
  case class InvokeContract(
      id: Transaction.ID,
      invoker: Account.ID,
      name: Contract.Name,
      version: Contract.Version,
      parameter: Contract.Parameter,
      signature: Signature,
      status: Transaction.Status
  ) extends Transaction {
    val sender: Account.ID                = invoker
    override def toBeVerified: BytesValue = {
      val buf = new StringBuilder
      buf.append(id.value)
      buf.append(invoker.value)
      buf.append(name.value)
      buf.append(version.value)
      buf.append(parameter.toString)
      BytesValue(buf.toString())
    }
  }

  // transaction id
  case class ID(value: String)

  // transaction status
  trait Status {
    def id: ID
  }

  object Status {
    case class Init(id: ID)     extends Status
    case class Rejected(id: ID) extends Status
    case class Pending(id: ID)  extends Status
    case class Failed(id: ID)   extends Status
  }
}
