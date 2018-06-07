package fssi.ast.domain.exceptions

case class ContractCompileError(msgs: Vector[String])
    extends FSSIException(
      msgs.mkString("\r\n")
    )
