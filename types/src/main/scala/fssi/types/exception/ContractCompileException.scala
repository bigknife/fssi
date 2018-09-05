package fssi
package types
package exception

case class ContractCompileException(messages: Vector[String])
    extends FSSIException(
      message =
        s"Compile contract occurred followed errors: ${messages.mkString("\n[\n", "\n", "\n]")}")
