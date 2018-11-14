package fssi.ast.uc
package corenode

import bigknife.sop._
import bigknife.sop.implicits._
import fssi.types.biz._

trait ProcessMessageProgram[F[_]] extends CoreNodeProgram[F] with BaseProgram[F] {
  import model._

  def processMessage(message: ConsensusAuxMessage): SP[F, Unit] = {
    for {
      lastDeterminedBlock <- store.getLatestDeterminedBlock()
      _                   <- consensus.processMessage(message, lastDeterminedBlock)
    } yield ()
  }
}
