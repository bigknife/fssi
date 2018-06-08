package fssi.ast.domain.exceptions

case class InsufficientBalance(accountId: String)
    extends FSSIException(s"Balance Of Account(id=$accountId) is Insufficient")
