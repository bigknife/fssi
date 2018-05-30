package fssi.ast.domain.types

import fssi.ast.domain.types.Contract.ZippedCodes

/** smart contract */
case class Contract(
    id: Contract.ID,
    name: Contract.Name,
    zippedCodes: ZippedCodes,
    codeSign: Signature,
    version: Contract.Version,
)

object Contract {
  case class ID(value: String)
  case class Name(value: String)
  case class Version()

  case class ZippedCodes(bytes: Array[Byte]) extends BytesValue

  /** parameters of contract */
  trait Parameter {}
}
