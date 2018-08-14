package fssi.types.exception

/**
  * Created on 2018/8/14
  */
case class ContractCompileError(messages: Vector[String])
    extends FSSIException(message = messages.mkString("\n"))
