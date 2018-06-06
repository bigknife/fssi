package fssi.ast.domain.types

/** smart contract */
case class Contract(
    name: Contract.Name,
    version: Contract.Version,
    code: Contract.Code,
    codeSign: Signature = Signature.Empty
) {
  def toBeVerified: BytesValue = {
    val buf = new StringBuilder
    buf.append(name.value)
    buf.append(version.value)
    buf.append(code.base64)
    BytesValue(buf.toString)
  }
}

object Contract {
  case class Name(value: String)
  case class Version(value: String)
  case class Code(base64: String)

  /** parameters of contract */
  trait Parameter {}
}
