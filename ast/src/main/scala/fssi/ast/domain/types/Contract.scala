package fssi.ast.domain.types

/** smart contract */
case class Contract(
    name: Contract.Name,
    version: Contract.Version,
    code: Contract.Code,
    codeSign: Signature = Signature.Empty
) {
  def toBeVerified: BytesValue = {
    val buf = new StringBuilder
    buf.append(name.value)
    buf.append(version.value)
    buf.append(code.base64)
    BytesValue(buf.toString)
  }
}

object Contract {
  case class Name(value: String)
  case class Version(value: String)
  case class Code(base64: String)

  /** parameters of contract */
  sealed trait Parameter {}
  object Parameter {

    sealed trait PrimaryParameter extends Parameter

    case class PString(value: String) extends PrimaryParameter {
      override def toString: String = value
    }
    case class PBigDecimal(value: java.math.BigDecimal) extends PrimaryParameter {
      override def toString: String = value.toPlainString
    }
    case class PBool(value: Boolean) extends PrimaryParameter {
      override def toString: String = value.toString
    }

    case class PArray(array: Array[PrimaryParameter]) extends Parameter {
      def :+(p: PrimaryParameter): PArray = PArray(array :+ p)

      override def toString: String = "[" + array.map(_.toString).mkString(",") + "]"
    }
    object PArray {
      val Empty: PArray = PArray(Array.empty[PrimaryParameter])
    }
  }

  // inner contract
  object inner {
    trait TransferContract{}
    object TransferContract {
      val name: Contract.Name = Contract.Name("fssi.ast.contract.inner.TransferContract")
      val version: Contract.Version = Contract.Version("0.0.1")
    }

    trait PublishContract{}
    object PublishContract {
      val name: Contract.Name = Contract.Name("fssi.ast.contract.inner.PublishContract")
      val version: Contract.Version = Contract.Version("0.0.1")
    }
  }
}
