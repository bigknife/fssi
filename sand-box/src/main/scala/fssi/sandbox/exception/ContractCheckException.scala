package fssi
package sandbox
package exception
import fssi.types.exception.FSSIException

case class ContractCheckException(messages: Vector[String])
    extends FSSIException(
      message =
        s"Check contract occurred followed errors: ${messages.mkString("\n[\n", "\n", "\n]")}")
