package fssi.ast.domain.exceptions

abstract class FSSIException(message: String, cause: Option[Throwable] = None)
    extends RuntimeException(message, if (cause.isDefined) cause.get else null)
