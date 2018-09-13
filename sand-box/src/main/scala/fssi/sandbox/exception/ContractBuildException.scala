package fssi
package sandbox
package exception
import fssi.types.exception.FSSIException

case class ContractBuildException(messages: Vector[String])
    extends FSSIException(
      message = s"build contract occur followed errors: ${messages.mkString("\n[\n", "\n", "\n]")}")
