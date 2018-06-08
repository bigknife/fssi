package fssi.ast.domain.exceptions

case class IllegalContractInvoke(name: String, version: String, msg: String)
    extends FSSIException(s"Can't Invoke Contract(name=$name,version=$version), $msg")
