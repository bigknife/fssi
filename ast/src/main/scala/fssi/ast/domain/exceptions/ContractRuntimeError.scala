package fssi.ast.domain.exceptions

case class ContractRuntimeError(name: String, version: String, cause: Throwable)
    extends FSSIException(s"Contract(name=$name,version=$version) running exception", Some(cause))
