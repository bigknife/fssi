package fssi
package ast

import types._, exception._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

/** log service */
@sp trait LogService[F[_]] {
  def debug(message: String, cause: Option[Throwable] = None): P[F, Unit]
  def info(message: String, cause: Option[Throwable] = None): P[F, Unit]
  def warn(message: String, cause: Option[Throwable] = None): P[F, Unit]
  def error(message: String, cause: Option[Throwable] = None): P[F, Unit]
}
