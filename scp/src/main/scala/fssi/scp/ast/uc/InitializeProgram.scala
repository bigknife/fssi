package fssi
package scp
package ast
package uc
import bigknife.sop._
import bigknife.sop.implicits._

trait InitializeProgram[F[_]] extends SCP[F] with BaseProgram[F] {

  import model._

  def initialize(): SP[F, Unit] = nodeService.cacheNodeQuorumSet()
}
