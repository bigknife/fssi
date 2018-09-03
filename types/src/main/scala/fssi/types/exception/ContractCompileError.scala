package fssi.types.exception

/**
  * Created on 2018/8/14
  */
case class ContractCompileError(messages: Vector[String])
    extends FSSIException(
      message =
        s"Contract compile occurred followed errors: ${messages.mkString("\n[\n", "\n", "\n]")}")
