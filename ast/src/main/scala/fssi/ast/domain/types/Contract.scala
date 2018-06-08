package fssi.ast.domain.types

import fssi.contract.States

/** smart contract */
sealed trait Contract {}

object Contract {
  case class UserContract(
      name: Contract.Name,
      version: Contract.Version,
      code: Contract.Code,
      codeHash: BytesValue = BytesValue.Empty
  ) extends Contract {
    def toBeHashed: BytesValue = {
      val buf = new StringBuilder
      buf.append(name.value)
      buf.append(version.value)
      buf.append(code.base64)
      BytesValue(buf.toString)
    }
  }

  case class Name(value: String)
  case class Version(value: String)
  case class Code(base64: String)
  case class Function(name: String)

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

    object PBigDecimal {
      def apply(value: Long): PBigDecimal = PBigDecimal(java.math.BigDecimal.valueOf(value))
    }

    case class PBool(value: Boolean) extends PrimaryParameter {
      override def toString: String = value.toString
    }

    case class PArray(array: Array[PrimaryParameter]) extends Parameter {
      def :+(p: PrimaryParameter): PArray = PArray(array :+ p)

      override def toString: String = "[" + array.map(_.toString).mkString(",") + "]"
    }
    object PArray {
      val Empty: PArray                        = PArray(Array.empty[PrimaryParameter])
      def apply(xs: PrimaryParameter*): PArray = PArray(xs.toArray)
    }
  }

  // inner contract
  object inner {
    //trait TransferContract extends Contract {}
    object TransferContract extends Contract {
      val name: Contract.Name       = Contract.Name("fssi.ast.contract.inner.TransferContract")
      val version: Contract.Version = Contract.Version("0.0.1")
    }

    //trait PublishContract extends Contract {}
    object PublishContract extends Contract {
      val name: Contract.Name       = Contract.Name("fssi.ast.contract.inner.PublishContract")
      val version: Contract.Version = Contract.Version("0.0.1")

      val ASSET_NAME: String = "fssi.ast.contract.inner.publishedcontract.____"
    }
  }
}
