package fssi
package sandbox
package exception
import fssi.types.exception.FSSIException

case class ContractCompileException(messages: Vector[String])
    extends FSSIException(
      message =
        s"Compile contract occurred followed errors: ${messages.mkString("\n[\n", "\n", "\n]")}")
