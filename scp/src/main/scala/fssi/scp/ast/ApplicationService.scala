package fssi.scp
package ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import scala.collection.immutable._

import types._


@sp trait ApplicationService[F[_]] {

  /** validate value on application level
    */
  def validateValue(value: Value): P[F, Boolean]
}
