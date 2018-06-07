package fssi.ast.domain.exceptions

case class IllegalContractParams(msg: Option[String] = None)
    extends FSSIException(msg.getOrElse("illegal parameters for smart contract"))
