package fssi
package types
package exception

case class ContractCheckException(messages: Vector[String])
    extends FSSIException(
      message =
        s"Check contract occurred followed errors: ${messages.mkString("\n[\n", "\n", "\n]")}")
