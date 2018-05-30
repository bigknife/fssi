package fssi.ast.domain

import bigknife.sop._, macros._,implicits._

/** log service */
@sp trait LogService[F[_]] {
  def debug(message: String, cause: Option[Throwable] = None): P[F, Unit]
  def info(message: String, cause: Option[Throwable] = None): P[F, Unit]
  def warn(message: String, cause: Option[Throwable] = None): P[F, Unit]
  def error(message: String, cause: Option[Throwable] = None): P[F, Unit]
}
