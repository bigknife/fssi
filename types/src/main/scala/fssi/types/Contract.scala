package fssi
package types

import fssi.types.Contract.Parameter.ReferenceParameter.PString
import fssi.types.Contract.ParameterType._

import scala.collection._
import scala.util.control.Exception.allCatch

/**
  * Contract is the engine to drive the chain world state to change
  */
sealed trait Contract {
  def name: UniqueName
  def version: Version
}

object Contract {

  trait Implicits {
    implicit val contractMethodOrdering: Ordering[Method] = new Ordering[Method] {
      def compare(x1: Method, x2: Method): Int =
        Ordering[String].compare(x1.toString, x2.toString)
    }
  }

  /** Contract method
    * there is no parameters info, so override methods can't be considered as a
    *  contract method.
    */
  case class Method(
      className: String,
      methodName: String
  ) {
    override def toString(): String = s"$className#$methodName"
  }

  /** Contract meta info
    */
  case class Meta(
      methods: immutable.TreeSet[Method]
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
    * Contract Method Parameter type
    */
  sealed trait ParameterType {
    def `type`: Class[_]
  }
  object ParameterType {
    case object TInt extends ParameterType {
      override def `type`: Class[_] = classOf[Int]
    }
    case object TLong extends ParameterType {
      override def `type`: Class[_] = classOf[Long]
    }
    case object TFloat extends ParameterType {
      override def `type`: Class[_] = classOf[Float]
    }
    case object TDouble extends ParameterType {
      override def `type`: Class[_] = classOf[Double]
    }
    case object TBool extends ParameterType {
      override def `type`: Class[_] = classOf[Boolean]
    }
    case object TString extends ParameterType {
      override def `type`: Class[_] = classOf[String]
    }
    case object TBigDecimal extends ParameterType {
      override def `type`: Class[_] = classOf[BigDecimal]
    }
    case object TArray extends ParameterType {
      override def `type`: Class[_] = classOf[Array[_]]
    }

    def apply(typeDescriptor: String): ParameterType = typeDescriptor match {
      case i if TInt.`type`.getSimpleName.equals(i)        => TInt
      case l if TLong.`type`.getSimpleName.equals(l)       => TLong
      case f if TFloat.`type`.getSimpleName.equals(f)      => TFloat
      case d if TDouble.`type`.getSimpleName.equals(d)     => TDouble
      case s if TString.`type`.getSimpleName.equals(s)     => TString
      case bool if TBool.`type`.getSimpleName.equals(bool) => TBool
      case b if TBigDecimal.`type`.getSimpleName.equals(b) => TBigDecimal
      case a if TArray.`type`.getSimpleName.equals(a)      => TArray
      case x                                               => throw new IllegalArgumentException(s"not support method parameter type $x")
    }
  }

  /**
    * Contract Method Parameter
    */
  sealed trait Parameter {
    def `type`: ParameterType
  }
  object Parameter {

    sealed trait PrimaryParameter extends Parameter {
      def value: Any
    }
    sealed trait ReferenceParameter extends Parameter

    object PrimaryParameter {
      case class PInt(value: Int) extends PrimaryParameter {
        override def `type`: ParameterType = TInt
      }
      case class PLong(value: Long) extends PrimaryParameter {
        override def `type`: ParameterType = TLong
      }
      case class PFloat(value: Float) extends PrimaryParameter {
        override def `type`: ParameterType = TFloat
      }
      case class PDouble(value: Double) extends PrimaryParameter {
        override def `type`: ParameterType = TDouble
      }
      case class PBool(value: Boolean) extends PrimaryParameter {
        override def `type`: ParameterType = TBool
      }

      def apply(parameter: String): PrimaryParameter = parameter match {
        case i if allCatch.opt(i.toInt).isDefined     => PInt(i.toInt)
        case l if allCatch.opt(l.toLong).isDefined    => PLong(l.toLong)
        case f if allCatch.opt(f.toFloat).isDefined   => PFloat(f.toFloat)
        case d if allCatch.opt(d.toDouble).isDefined  => PDouble(d.toDouble)
        case b if allCatch.opt(b.toBoolean).isDefined => PBool(b.toBoolean)
        case x                                        => PString(x)
      }

    }
    object ReferenceParameter {
      case class PString(value: String) extends PrimaryParameter {
        override def toString: String      = value
        override def `type`: ParameterType = TString
      }
      case class PBigDecimal(value: java.math.BigDecimal) extends PrimaryParameter {
        override def toString: String      = value.toPlainString
        override def `type`: ParameterType = TBigDecimal
      }
      object PBigDecimal {
        def apply(value: Long): PBigDecimal = PBigDecimal(java.math.BigDecimal.valueOf(value))
      }

      case class PArray(array: Array[PrimaryParameter]) extends Parameter {
        def :+(p: PrimaryParameter): PArray = PArray(array :+ p)
        override def toString: String       = "[" + array.map(_.toString).mkString(",") + "]"
        override def `type`: ParameterType  = TArray
      }
      object PArray {
        def empty[T]: PArray                        = PArray(Array.empty[PrimaryParameter])
        def apply[T](xs: PrimaryParameter*): PArray = PArray(xs.toArray)
      }
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
