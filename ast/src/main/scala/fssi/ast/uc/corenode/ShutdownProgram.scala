package fssi.ast
package uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.base._
import fssi.types.biz._
import java.io._

trait ShutdownProgram[F[_]] extends CoreNodeProgram[F] with BaseProgram[F] {
  import model._

  def shutdown(consensusNode: Node.ConsensusNode, applicationNode: Option[Node.ApplicationNode]): SP[F, Unit] = {
    for {
      _ <- consensus.destroy()
      _ <- store.unload()
      _ <- contract.closeRuntime()
      _ <- network.shutdownConsensusNode(consensusNode)
      _ <- ifThen(applicationNode.isDefined)(network.shutdownApplicationNode(applicationNode.get))
    } yield ()

  }

}
