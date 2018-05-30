package fssi.ast.domain.exceptions

case class IllegalSignature(tag: String) extends FSSIException(s"[$tag] illegal signature")
