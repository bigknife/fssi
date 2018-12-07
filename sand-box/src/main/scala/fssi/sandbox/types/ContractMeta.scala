package fssi
package sandbox
package types
import fssi.sandbox.types.ContractMeta._

case class ContractMeta(
    owner: Owner,
    name: Name,
    version: Version,
    description: Description,
    interfaces: Vector[MethodDescriptor]
)

object ContractMeta {

  case class Owner(value: String)

  case class Name(value: String)

  case class Version(value: String)

  case class Description(value: String)

  case class MethodDescriptor(alias: String, descriptor: String)

}
