package fssi
package sandbox
package exception
import fssi.types.exception.FSSIException

case class SandBoxEnvironmentException(message: String) extends FSSIException(message = message)
