package fssi.types
package biz

import base._
import fssi.base.BytesValue
import fssi.types.biz.Contract.UserContract.Parameter.Description

import scala.collection._
import fssi.types.implicits._

sealed trait Contract {
  def name: UniqueName
  def version: Contract.Version
}

object Contract {

  /** Version, a subset of SemVer. DOESN'T support pre-release and build meta.
    *  ref: SemVer https://semver.org/
    */
  case class Version(major: Int, minor: Int, patch: Int) {
    override def toString: String = s"$major.$minor.$patch"

    def >(version: Version): Boolean =
      major > version.major || (major == version.major && minor > version.minor) || (major == version.major && minor == version.minor && patch > version.patch)
  }
  object Version {

    private val VersionR = "^(\\d+)\\.(\\d+)\\.(\\d+)$".r

    /** empty or initialization version
      */
    def empty: Version = Version(0, 0, 0)
    def apply(s: String): Option[Version] = s match {
      case VersionR(major, minor, patch) => Some(Version(major.toInt, minor.toInt, patch.toInt))
      case _                             => None
    }

    trait Implicits {

      /** version is ordered
        */
      implicit val versionOrdering: Ordering[Version] = new Ordering[Version] {
        def compare(x1: Version, x2: Version): Int = {
          val oi = Ordering[Int]
          val m  = oi.compare(x1.major, x2.major)
          if (m == 0) {
            val mi = oi.compare(x1.minor, x2.minor)
            if (mi == 0) {
              oi.compare(x1.patch, x2.patch)
            } else mi
          } else m
        }
      }
      implicit def versionToBytesValue(v: Version): Array[Byte] = v.toString.getBytes("utf-8")
    }
  }

  // user contract
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
      code: UserContract.Code,
      methods: immutable.TreeSet[UserContract.Method],
      description: Description,
      signature: Signature
  ) extends Contract
  object UserContract {
    case class Code(value: Array[Byte])
    case class Method(
        alias: String,
        fullSignature: String
    ) {
      override def toString(): String = {
        s"$alias=$fullSignature"
      }
    }
    object Method {
      def empty: Method = Method("", "")
      def parse(s: String): Option[Method] = s.split("=") match {
        case Array(alias, fs) => Some(Method(alias, fs))
        case _                => None
      }
    }

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
        def empty: PArray                        = PArray(Array.empty[PrimaryParameter])
        def apply(xs: PrimaryParameter*): PArray = PArray(xs.toArray)
      }

      case class Description(value: Array[Byte])
    }

    def methodsToDeterminedBytes(
        methods: scala.collection.immutable.TreeSet[UserContract.Method]): Array[Byte] =
      methods.map(_.toString()).mkString("\n").getBytes("utf-8")

    def bytesToMethods(
        bytes: Array[Byte]): scala.collection.immutable.TreeSet[UserContract.Method] =
      scala.collection.immutable.TreeSet(
        new String(bytes, "utf-8")
          .split("\n")
          .map(x => Method.parse(x).get): _*)

    def toDeterminedBytes(uc: UserContract): Array[Byte] = {
      val owner     = uc.owner.asBytesValue.bcBase58
      val name      = uc.name.asBytesValue.bcBase58
      val version   = uc.version.asBytesValue.bcBase58
      val code      = uc.code.asBytesValue.bcBase58
      val methods   = methodsToDeterminedBytes(uc.methods).asBytesValue.bcBase58
      val desc      = uc.description.asBytesValue.bcBase58
      val signature = uc.signature.asBytesValue.bcBase58
      Vector(owner, name, version, code, methods, desc, signature).mkString("\n").getBytes("utf-8")
    }
    def fromDeterminedBytes(bytes: Array[Byte]): UserContract = {
      new String(bytes, "utf-8").split("\n") match {
        case Array(owner, name, version, code, methods, desc, signature) =>
          UserContract(
            owner = Account.ID(BytesValue.decodeBcBase58(owner).get.bytes),
            name = UniqueName(BytesValue.decodeBcBase58(name).get.bytes),
            version = Version(new String(BytesValue.decodeBcBase58(version).get.bytes, "utf-8")).get,
            code = Code(BytesValue.decodeBcBase58(code).get.bytes),
            methods = bytesToMethods(BytesValue.decodeBcBase58(methods).get.bytes),
            description = Description(BytesValue.decodeBcBase58(desc).get.bytes),
            signature = Signature(BytesValue.decodeBcBase58(signature).get.bytes)
          )
        case _ => throw new RuntimeException("insance user contract data")
      }
    }

    def parameterToDeterminedBytes(p: Parameter): Array[Byte] = p match {
      case Parameter.PString(x) =>
        Vector("PString", x)
          .mkString("\n")
          .asBytesValue
          .bcBase58
          .getBytes("utf-8")

      case Parameter.PBigDecimal(x) =>
        Vector("PBigDecimal", x.toPlainString)
          .mkString("\n")
          .asBytesValue
          .bcBase58
          .getBytes("utf-8")

      case Parameter.PBool(x) =>
        Vector("PBool", if (x) "true" else "false")
          .mkString("\n")
          .asBytesValue
          .bcBase58
          .getBytes("utf-8")

      case Parameter.PArray(xs) =>
        Vector("PArray",
               xs.foldLeft(Vector.empty[String]) { (acc, n) =>
                   acc :+ new String(parameterToDeterminedBytes(n), "utf-8")
                 }
                 .mkString("\n")
                 .asBytesValue
                 .bcBase58).mkString("\n").asBytesValue.bcBase58.getBytes("utf-8")
    }
    def parameterFromDeterminedBytes(bytes: Array[Byte]): Option[Parameter] = {
      BytesValue.decodeBcBase58[Any](new String(bytes, "utf-8")).map { x =>
        import Parameter._
        new String(x.bytes, "utf-8").split("\n") match {
          case Array("PString", x0)     => PString(x0)
          case Array("PBigDecimal", x0) => PBigDecimal(new java.math.BigDecimal(x0))
          case Array("PBool", "true")   => PBool(true)
          case Array("PBool", "false")  => PBool(false)
          case Array("PArray", x0) =>
            val xs: Array[Parameter.PrimaryParameter] =
              new String(BytesValue.unsafeDecodeBcBase58(x0).bytes, "utf-8")
                .split("\n")
                .map(BytesValue.unsafeDecodeBcBase58)
                .map(_.bytes)
                .map(parameterFromDeterminedBytes)
                .filter(_.isDefined)
                .map(_.get.asInstanceOf[Parameter.PrimaryParameter])
            PArray(xs)
        }
      }
    }

    trait Implicits {
      implicit val methodOrdering: Ordering[Method] = new Ordering[Method] {
        def compare(m1: Method, m2: Method): Int =
          Ordering[String].compare(m1.toString, m2.toString)
      }

      implicit def methodToBytesValue(m: Method): Array[Byte] = m.toString.getBytes

      implicit def codeToBytesValue(c: Code): Array[Byte] = c.value

      implicit def parameterToBytesValue(p: Parameter): Array[Byte] =
        parameterToDeterminedBytes(p)

      implicit def decToBytesValue(dec: Description): Array[Byte] = dec.value
    }

    implicit def userContractToBytesValue(uc: UserContract): Array[Byte] =
      toDeterminedBytes(uc)
    /*{
        import uc._
        owner.value ++ name.value ++ version.asBytesValue.bytes ++ code.value ++ methods.foldLeft(Array.emptyByteArray){(acc, n) =>
          acc ++ n.asBytesValue.bytes
        }
      }
    }*/
  }

  // inner contract
  object inner {
    // inner contract current version is 1.0.0
    val CURRENT_VERSION = Version(1, 0, 0)
    //trait TransferContract extends Contract {}
    object TransferContract extends Contract {
      val name: UniqueName = UniqueName("fssi.types.contract.inner.TransferContract")
      val version: Version = CURRENT_VERSION
    }

    //trait PublishContract extends Contract {}
    object DeployContract extends Contract {
      val name: UniqueName = UniqueName("fssi.ast.contract.inner.PublishContract")
      val version: Version = CURRENT_VERSION
    }
  }
}
