package fssi.ast.uc
package corenode

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.base._
import fssi.types.biz._
import java.io._

trait StartupProgram[F[_]] extends CoreNodeProgram[F] with BaseProgram[F] {
  import model._

  def startup(root: File, handler: JsonMessageHandler): SP[F, Node] = {
    for {
      _         <- contract.assertRuntime()
      _         <- contract.initializeRuntime()
      _         <- log.info("contract runtime checking passed.")
      _         <- store.loadAndCheck(root)
      _         <- log.info("store loaded, and checking passed.")
      chainConf <- store.getChainConfiguration()
      node      <- network.startupPeerNode(chainConf, handler)
      _         <- log.info("network startup.")
      _         <- consensus.initialize(node)
      _         <- log.info("consensus engine initialized.")
      _         <- log.info("CoreNode startup!")
    } yield node
  }

  
}
