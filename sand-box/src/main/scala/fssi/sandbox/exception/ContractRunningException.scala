package fssi
package sandbox
package exception
import fssi.types.exception.FSSIException

case class ContractRunningException(messages: Vector[String])
    extends FSSIException(
      message =
        s"run smart contract occurred followed errors: ${messages.mkString("\n[\n", "\n", "\n]")}"
    )
