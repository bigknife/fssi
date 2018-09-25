package fssi.types
package biz

import base._
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
  }
  object Version {

    private val VersionR = "^(\\d+)\\.(\\d+)\\.(\\d+)$".r

    /** empty or initialization version
      */
    def empty: Version = Version(0, 0, 0)
    def apply(s: String): Option[Version] = s match {
      case VersionR(major, minor, patch) => Some(Version(major.toInt, minor.toInt, patch.toInt))
      case _ => None
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
      implicit def versionToBytesValue(v: Version): Array[Byte] = v.toString.getBytes
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
    }

    trait Implicits {
      implicit val methodOrdering: Ordering[Method] = new Ordering[Method] {
        def compare(m1: Method, m2: Method): Int = Ordering[String].compare(m1.toString, m2.toString)
      }

      implicit def methodToBytesValue(m: Method): Array[Byte] = m.toString.getBytes

      implicit def codeToBytesValue(c: Code): Array[Byte] = c.value

      implicit def parameterToBytesValue(p: Parameter): Array[Byte] = p match {
        case Parameter.PString(x) => x.getBytes("utf-8")
        case Parameter.PBigDecimal(x) => x.toPlainString.getBytes("utf-8")
        case Parameter.PBool(x) => if(x) Array(1) else Array(0)
        case Parameter.PArray(xs) => xs.foldLeft(Array.emptyByteArray) {(acc, n) =>
          acc ++ parameterToBytesValue(n)
        }
      }

      implicit def userContractToBytesValue(uc: UserContract): Array[Byte] = {
        import uc._
        owner.value ++ name.value ++ version.asBytesValue.bytes ++ code.value ++ methods.foldLeft(Array.emptyByteArray){(acc, n) =>
          acc ++ n.asBytesValue.bytes
        }
      }
    }
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
