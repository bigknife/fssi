package fssi.types

import fssi.utils._

/**
  * Contract is the engine to drive the chain world state to change
  */
sealed trait Contract {
  def name: UniqueName
  def version: Version
}

object Contract {

  /** Contract method
    * there is no parameters info, so override methods can't be considered as a
    *  contract method.
    */
  case class Method(
      className: String,
      methodName: String
  )

  /** Contract meta info
    */
  case class Meta(
      methods: Vector[Method]
  )

  /**
    * Smart contract info
    * @param owner the owner's account public key a hex string.
    * @param name the full identified path of this contract
    * @param code the smart contract code
    */
  case class UserContract(
      owner: Account.ID,
      name: UniqueName,
      version: Version,
      code: Base64String,
      meta: Contract.Meta,
      signature: Signature
  ) extends Contract

  /**
    * Contract Method Parameter
    */
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
    val CURRENT_VERSION = "0.1.0"
    //trait TransferContract extends Contract {}
    object TransferContract extends Contract {
      val name: UniqueName = UniqueName("fssi.types.contract.inner.TransferContract")
      val version: Version = Version(CURRENT_VERSION)
    }

    //trait PublishContract extends Contract {}
    object PublishContract extends Contract {
      val name: UniqueName = UniqueName("fssi.ast.contract.inner.PublishContract")
      val version: Version = Version("0.0.1")
    }
  }
}
