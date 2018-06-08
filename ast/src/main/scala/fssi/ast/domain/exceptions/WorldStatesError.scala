package fssi.ast.domain.exceptions

case class WorldStatesError(accountId: String, cause: Option[Throwable] = None)
    extends FSSIException(s"World State Error for: accountId = $accountId", cause)
