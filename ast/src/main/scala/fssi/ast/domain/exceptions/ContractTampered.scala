package fssi.ast.domain.exceptions

case class ContractTampered(name: String, version: String)
    extends FSSIException(s"Contract(name=$name,version=$version) Has Been Tampered")
