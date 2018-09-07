package fssi
package ast
package uc

import types._
import utils._
import types.syntax._
import bigknife.sop._
import bigknife.sop.implicits._
import java.io.File
import java.nio.file.Path

import fssi.types.Contract.Parameter

trait ToolProgram[F[_]] extends BaseProgram[F] {
  import model._

  /** Create an account, only a password is needed.
    * NOTE: then password is ensured to be 24Bytes length.
    */
  def createAccount(password: String): SP[F, Account] = {
    import crypto._
    for {
      keypair <- createKeyPair()
      (publicKey, privateKey) = keypair
      iv <- createIVForDes()
      pk <- desEncryptPrivateKey(privateKey, iv, password = password.getBytes("utf-8"))
    } yield Account(HexString(publicKey.value), HexString(pk.value), HexString(iv.value))
  }

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
  }

  /** Compile contract
    * @param sandboxVersion version of the sandbox for smart contract running on.
    */
  def compileContract(projectDirectory: File, output: File, sandboxVersion: String): SP[F, Unit] = {
    import contractService._
    for {
      determinEither <- checkDeterminismOfContractProject(projectDirectory)
      _              <- err.either(determinEither)
      compileEither  <- compileContractProject(projectDirectory, sandboxVersion, output)
      _              <- err.either(compileEither)
    } yield ()
  }

  /** Create a transfer transaction json rpc protocol
    */
  def createTransferTransaction(accountFile: File,
                                password: Array[Byte],
                                payee: Account.ID,
                                token: Token): SP[F, Transaction.Transfer] = {
    import accountStore._
    import crypto._
    import transactionService._
    for {
      accountOrFailed <- loadAccountFromFile(accountFile)
      account         <- err.either(accountOrFailed)
      privateKeyOrFailed <- desDecryptPrivateKey(account.encryptedPrivateKey.toBytesValue,
                                                 account.iv.toBytesValue,
                                                 BytesValue(password))
      privateKey        <- err.either(privateKeyOrFailed)
      transferNotSigned <- createUnsignedTransfer(payer = account.id, payee, token)
      unsignedBytes     <- calculateSingedBytesOfTransaction(transferNotSigned)
      signature         <- makeSignature(unsignedBytes, privateKey)
    } yield transferNotSigned.copy(signature = signature)
  }

  def createPublishContractTransaction(
      accountFile: File,
      password: Array[Byte],
      contractFile: File,
      contractName: UniqueName,
      contractVersion: Version): SP[F, Transaction.PublishContract] = {
    import accountStore._
    import crypto._
    import transactionService._
    import contractService._

    for {
      accountOrFailed <- loadAccountFromFile(accountFile)
      account         <- err.either(accountOrFailed)
      privateKeyOrFailed <- desDecryptPrivateKey(account.encryptedPrivateKey.toBytesValue,
                                                 account.iv.toBytesValue,
                                                 BytesValue(password))
      privateKey <- err.either(privateKeyOrFailed)
      userContractOrFailed <- createUserContractFromContractFile(account,
                                                                 contractFile,
                                                                 contractName,
                                                                 contractVersion)
      userContract             <- err.either(userContractOrFailed)
      publishContractNotSigned <- createUnsignedPublishContractTransaction(account.id, userContract)
      unsignedBytes            <- calculateSingedBytesOfTransaction(publishContractNotSigned)
      signature                <- makeSignature(unsignedBytes, privateKey)
    } yield publishContractNotSigned.copy(signature = signature)
  }

  def createRunContractTransaction(
      accountFile: File,
      password: Array[Byte],
      contractName: UniqueName,
      contractVersion: Version,
      method: Contract.Method,
      parameter: Contract.Parameter): SP[F, Transaction.RunContract] = {
    import accountStore._
    import crypto._
    import transactionService._
    import contractService._

    for {
      accountOrFailed <- loadAccountFromFile(accountFile)
      account         <- err.either(accountOrFailed)
      privateKeyOrFailed <- desDecryptPrivateKey(account.encryptedPrivateKey.toBytesValue,
                                                 account.iv.toBytesValue,
                                                 BytesValue(password))
      privateKey <- err.either(privateKeyOrFailed)
      runContractNotSigned <- createUnsignedRunContractTransaction(account.id,
                                                                   contractName,
                                                                   contractVersion,
                                                                   method,
                                                                   parameter)
      unsignedBytes <- calculateSingedBytesOfTransaction(runContractNotSigned)
      signature     <- makeSignature(unsignedBytes, privateKey)
    } yield runContractNotSigned.copy(signature = signature)
  }
}

object ToolProgram {
  def apply[F[_]](implicit M: components.Model[F]): ToolProgram[F] = new ToolProgram[F] {
    val model: components.Model[F] = M
  }
}
