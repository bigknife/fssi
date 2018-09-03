package fssi
package sandbox

case class SandBoxVersion(value: Int) {

  private lazy val STEPPER: Int = 1000

  def isVersionValid: Boolean = value < 1000

  def toInnerVersion: SandBoxVersion = SandBoxVersion(value = value + STEPPER)

  def toOuterVersion: SandBoxVersion = SandBoxVersion(value = value - STEPPER)
}
