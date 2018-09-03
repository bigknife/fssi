package fssi
package types
package exception

case class ContractCompileError(messages: Vector[String])
    extends FSSIException(
      message =
        s"Contract compile occurred followed errors: ${messages.mkString("\n[\n", "\n", "\n]")}")
