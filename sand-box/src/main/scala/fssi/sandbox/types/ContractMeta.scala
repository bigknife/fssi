package fssi
package sandbox
package types

case class Owner(value: String)

case class Name(value: String)

case class Version(value: String)

case class MethodDescriptor(alias: String, descriptor: String)

case class ContractMeta(
    owner: Owner,
    name: Name,
    version: Version,
    interfaces: Vector[MethodDescriptor]
)
