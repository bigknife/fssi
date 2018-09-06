package fssi
package sandbox
import scala.util.control.Exception._

private[sandbox] case class SandBoxVersion(value: Int) {

  private lazy val STEPPER: Int = 1000

  def isVersionValid: Boolean = SandBoxVersion.majVersionRange.contains(toOuterVersion.value)

  def toInnerVersion: SandBoxVersion = SandBoxVersion(value = value + STEPPER)

  def toOuterVersion: SandBoxVersion = SandBoxVersion(value = value - STEPPER)
}

object SandBoxVersion {

  lazy val javaVersionRange: Range = 6 to 10
  lazy val majVersionRange: Range  = 50 to 54

  private lazy val majVersionMap: IndexedSeq[(Int, Int)] = javaVersionRange.zip(majVersionRange)

  def apply(value: String): Option[SandBoxVersion] = value match {
    case i
        if allCatch
          .opt(i.toInt)
          .isDefined && javaVersionRange.contains(i.toInt) =>
      Some(SandBoxVersion(majVersionMap.find(v => v._1 == i.toInt).get._2))
    case _ => None
  }
}
