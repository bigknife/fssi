package fssi
package sandbox
package types

import scala.util.control.Exception._

case class SandBoxVersion(maj: Int, min: Int, patch: Int) {

  def gteTo(version: SandBoxVersion): Boolean =
    maj >= version.maj || (maj == version.maj && min >= version.min) || (version.maj == maj && version.min == min && patch >= version.patch)

  def lteTo(version: SandBoxVersion): Boolean =
    maj <= version.maj || (version.maj == maj && min <= version.min) || (version.maj == maj && version.min == min && patch <= version.patch)

  def toInnerVersion(underlyingVersion: Int): Int = underlyingVersion + maj * 100 + min * 10 + patch

  def toOuterVersion(innVersion: Int): Int = innVersion - maj * 100 - min * 10 - patch

  def supportHighestJavaVersion: Int = SandBoxVersion.supportHighestJavaVersion(this)

  override def toString: String = s"$maj.$min.$patch"
}

object SandBoxVersion {

  import Protocol._

  private[sandbox] lazy val currentVersion = SandBoxVersion(version).get

  def apply(value: String): Option[SandBoxVersion] = value.split("\\.") match {
    case Array(maj, min, patch)
        if allCatch.opt(maj.toInt).isDefined && allCatch.opt(min.toInt).isDefined && allCatch
          .opt(patch.toInt)
          .isDefined =>
      Some(SandBoxVersion(maj.toInt, min.toInt, patch.toInt))
    case _ => None
  }

  private[SandBoxVersion] lazy val javaVersionMatch = Map(majVersion -> majJavaVersion)

  def supportHighestJavaVersion(sandBoxVersion: SandBoxVersion): Int =
    javaVersionMatch(sandBoxVersion.maj)
}
