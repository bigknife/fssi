package fssi
package ast
package uc
package tool

import fssi.types.biz._
import fssi.types.base._

import bigknife.sop._
import bigknife.sop.implicits._
import types.implicits._

import java.io._

trait LocalChainProgram[F[_]] extends BaseProgram[F] {
  import model._
    /** Create a chain
    * @param dataDir directory where the chain data saved
    * @param chainID the chain id
    */
  def createChain(dataDir: File, chainID: String): SP[F, Unit] = {
    import chainStore._
    import blockStore._
    import tokenStore._
    import contractStore._
    import contractDataStore._
    import blockService._
    import log._
    /*
    for {
      createRoot   <- createChainRoot(dataDir, chainID)
      root         <- err.either(createRoot)
      confFile     <- createDefaultConfigFile(root)
      _            <- initializeBlockStore(root)
      _            <- initializeTokenStore(root)
      _            <- initializeContractStore(root)
      _            <- initializeContractDataStore(root)
      genesisBlock <- createGenesisBlock(chainID)
      _            <- saveBlock(genesisBlock)
      _            <- info(s"chain initialized, please edit the default config file: $confFile")
    } yield ()
     */
    ???
  }

}
