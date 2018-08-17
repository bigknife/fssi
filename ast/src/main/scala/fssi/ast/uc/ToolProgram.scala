package fssi
package ast
package uc

import types._
import utils._
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
    } yield Account(HexString(publicKey.value), HexString(pk.value), HexString(iv.value))
  }

  /** Create a chain
    * @param dataDir directory where the chain data saved
    * @param chainID the chain id
    */
  def createChain(dataDir: File, chainID: String): SP[F, Unit] = {
    for {
      createRoot   <- chainStore.createChainRoot(dataDir, chainID)
      root         <- err.either(createRoot)
      confFile     <- chainStore.createDefaultConfigFile(root)
      _            <- blockStore.initialize(root)
      _            <- tokenStore.initialize(root)
      _            <- contractStore.initialize(root)
      _            <- contractDataStore.initialize(root)
      genesisBlock <- blockService.createGenesisBlock(chainID)
      _            <- blockStore.saveBlock(genesisBlock)
      _            <- log.info(s"chain initialized, please edit the default config file: $confFile")
    } yield ()
  }

  /** Create a transfer transaction json rpc protocol
    */
  def createTransferTransaction(accountFile: File,
                                password: Array[Byte],
                                payee: Account.ID,
                                token: Token): SP[F, Transaction.Transfer] = {
    for {
      accountOrFailed <- accountStore.loadAccountFromFile(accountFile)
      account         <- err.either(accountOrFailed)
      privateKeyOrFailed <- crypto.desDecryptPrivateKey(account.encryptedPrivateKey.toBytesValue,
                                                        account.iv.toBytesValue,
                                                        BytesValue(password))
      privateKey <- err.either(privateKeyOrFailed)
      transferNotSigned <- transactionService.createUnsignedTransfer(payer = account.id,
                                                                     payee,
                                                                     token)
      unsignedBytes <- transactionService.toBeSingedBytesOfTransfer(transferNotSigned)
      signature     <- crypto.makeSignature(unsignedBytes, privateKey)
    } yield transferNotSigned.copy(signature = signature)
  }
}

object ToolProgram {
  def apply[F[_]](implicit M: components.Model[F]): ToolProgram[F] = new ToolProgram[F] {
    val model: components.Model[F] = M
  }
}
