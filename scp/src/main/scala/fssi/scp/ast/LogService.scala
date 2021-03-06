package fssi.scp
package ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import types._

@sp trait LogService[F[_]] {
  def debug(message: String, cause: Option[Throwable] = None): P[F, Unit]
  def info(message: String, cause: Option[Throwable] = None): P[F, Unit]
  def warn(message: String, cause: Option[Throwable] = None): P[F, Unit]
  def error(message: String, cause: Option[Throwable] = None): P[F, Unit]

  def infoReceivedEnvelope[M <: Message](envelope: Envelope[M]): P[F, Unit]
  def infoSentEnvelope[M <: Message](envelope: Envelope[M]): P[F, Unit]
}
