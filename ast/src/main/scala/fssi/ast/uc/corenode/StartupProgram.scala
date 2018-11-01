package fssi.ast.uc
package corenode

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types._
import fssi.types.base._
import fssi.types.biz._
import java.io._

trait StartupProgram[F[_]] extends CoreNodeProgram[F] with BaseProgram[F] {
  import model._

  /** Start up a full-functioning core node.
    * runing consensus node and application node
    * @return node info
    */
  def startupFull(root: File,
              consensusMessageHandler: Message.Handler[ConsensusMessage],
              applicationMessageHandler: Message.Handler[ApplicationMessage]): SP[F, (Node.ConsensusNode, Node.ApplicationNode)] = {
    for {
      _               <- contract.assertRuntime()
      _               <- contract.initializeRuntime()
      _               <- log.info("contract runtime checking passed.")
      _               <- store.loadAndCheck(root)
      _               <- log.info("store loaded, and checking passed.")
      chainConf       <- store.getChainConfiguration()
      consensusNode   <- network.startupConsensusNode(chainConf, consensusMessageHandler)
      applicationNode <- network.startupApplicationNode(chainConf, applicationMessageHandler)
      _               <- log.info("network startup.")
      _               <- consensus.initialize(consensusNode)
      _               <- log.info("consensus engine initialized.")
      _               <- log.info("CoreNode startup!")
    } yield (consensusNode, applicationNode)
  }

  /** Start up a semi-functioning core node.
    * runing consensus node only
    * @return node info
    */
  def startupSemi(root: File,
              consensusMessageHandler: Message.Handler[ConsensusMessage]): SP[F, Node.ConsensusNode] = {
    for {
      _             <- contract.assertRuntime()
      _             <- contract.initializeRuntime()
      _             <- log.info("contract runtime checking passed.")
      _             <- store.loadAndCheck(root)
      _             <- log.info("store loaded, and checking passed.")
      chainConf     <- store.getChainConfiguration()
      consensusNode <- network.startupConsensusNode(chainConf, consensusMessageHandler)
      _             <- log.info("network startup.")
      _             <- consensus.initialize(consensusNode)
      _             <- log.info("consensus engine initialized.")
      _             <- log.info("CoreNode startup!")
    } yield consensusNode
  }
}
