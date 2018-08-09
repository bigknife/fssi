package fssi
package ast
package uc

import types._
import types.syntax._
import bigknife.sop._
import bigknife.sop.implicits._

import java.io.File

trait ToolProgram[F[_]] {
  val model: components.Model[F]
  import model._

  /** Create an account, only a password is needed.
    * NOTE: then password is ensured to be 24Bytes length.
    */
  def createAccount(password: String): SP[F, Account] = {
    for {
      keypair <- crypto.createKeyPair()
      (publicKey, privateKey) = keypair
      iv <- crypto.createIVForDes()
      pk <- crypto.desEncryptPrivateKey(privateKey, iv, password = password.getBytes("utf-8"))
    } yield Account(publicKey.toHexString, pk.toHexString, iv.toHexString)
  }

  /** Create a chain
    * @param dataDir directory where the chain data saved
    * @param chainID the chain id
    */
  def createChain(dataDir: File, chainID: String): SP[F, Unit] = {
    for {
      _            <- blockStore.initialize(dataDir)
      _            <- tokenStore.initialize(dataDir)
      _            <- contractStore.initialize(dataDir)
      _            <- contractDataStore.initialize(dataDir)
      genesisBlock <- blockService.createGenesisBlock(chainID)
      _            <- blockStore.saveBlock(genesisBlock)
    } yield ()
  }
}

object ToolProgram {
  def apply[F[_]](implicit M: components.Model[F]): ToolProgram[F] = new ToolProgram[F] {
    val model: components.Model[F] = M
  }
}
