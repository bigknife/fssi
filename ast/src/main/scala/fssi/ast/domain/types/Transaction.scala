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

  def bytes: Array[Byte]
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

    lazy val bytes: Array[Byte] = {
      id.value.getBytes("utf-8") ++
        from.value.getBytes("utf-8") ++
        to.value.getBytes("utf-8") ++
        amount.toString.getBytes("utf-8") ++
        signature.bytes ++
        status.toString.getBytes("utf-8")
    }
  }

  // publish a contract
  case class PublishContract(
      id: Transaction.ID,
      owner: Account.ID,
      contract: Contract.UserContract,
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

    lazy val bytes: Array[Byte] = {
      id.value.getBytes("utf-8") ++
        owner.value.getBytes("utf-8") ++
        contract.toBeSigned.bytes ++
        signature.bytes ++
        status.toString.getBytes("utf-8")
    }
  }

  // invoke a contract
  case class InvokeContract(
      id: Transaction.ID,
      invoker: Account.ID,
      name: Contract.Name,
      version: Contract.Version,
      function: Contract.Function,
      parameter: Contract.Parameter,
      signature: Signature,
      status: Transaction.Status
  ) extends Transaction {
    val sender: Account.ID = invoker
    override def toBeVerified: BytesValue = {
      val buf = new StringBuilder
      buf.append(id.value)
      buf.append(invoker.value)
      buf.append(name.value)
      buf.append(version.value)
      buf.append(parameter.toString)
      BytesValue(buf.toString())
    }

    lazy val bytes: Array[Byte] = {
      id.value.getBytes("utf-8") ++
        invoker.value.getBytes("utf-8") ++
        name.value.getBytes("utf-8") ++
        version.value.getBytes("utf-8") ++
        function.name.getBytes("utf-8") ++
        parameter.toString.getBytes("utf-8") ++
        signature.bytes ++
        status.toString.getBytes("utf-8")
    }
  }

  // transaction id
  case class ID(value: String)

  // transaction status
  trait Status {
    def id: ID
  }

  object Status {
    case class Init(id: ID) extends Status {
      override def toString: String = s"Init(${id.value})"
    }
    case class Rejected(id: ID) extends Status {
      override def toString: String = s"Rejected(${id.value})"
    }
    case class Pending(id: ID) extends Status {
      override def toString: String = s"Pending(${id.value})"
    }
    case class Failed(id: ID) extends Status {
      override def toString: String = s"Failed(${id.value})"
    }
  }

  def initStatus(id: ID): Status     = Status.Init(id)
  def rejectedStatus(id: ID): Status = Status.Rejected(id)
  def pendingStatus(id: ID): Status  = Status.Pending(id)
  def failedStatus(id: ID): Status   = Status.Failed(id)
}
