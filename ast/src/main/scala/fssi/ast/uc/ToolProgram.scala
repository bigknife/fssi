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

  /***
    * compile smart contract
    * @param sourceDir path to read contract source code
    * @param destDir path to store contract zip
    */
  def compileContract(sourceDir: Path, destDir: Path, format: CodeFormat): SP[F, Unit] = {
    for {
      classPathEither   <- contractService.compileContractSourceCode(sourceDir)
      classPath         <- err.either(classPathEither)
      determinismEither <- contractService.checkDeterministicOfClass(classPath)
      _                 <- err.either(determinismEither)
      bytesValue        <- contractService.zipContract(classPath)
      _                 <- contractService.outputZipFile(bytesValue, destDir, format)
    } yield ()
  }

  /***
    * run smart contract
    * @param classesDir contract classes dir
    * @param clazzName concrete qualified class name
    * @param methodName method name in clazz name
    * @param parameters parameters for method $methodName
    * @param decodeFormat decode format of contract classes
    * @return
    */
  def runContract(classesDir: Path,
                  clazzName: String,
                  methodName: String,
                  parameters: Array[Parameter],
                  decodeFormat: CodeFormat): SP[F, Unit] = {
    for {
      codeBytes   <- contractService.decodeContractClasses(classesDir, decodeFormat)
      contractDir <- contractService.buildContractDir(codeBytes)
      checkEither <- contractService.checkContractMethod(contractDir,
                                                         clazzName,
                                                         methodName,
                                                         parameters)
      _ <- err.either(checkEither)
      invokeEither <- contractService.invokeContractMethod(contractDir,
                                                           clazzName,
                                                           methodName,
                                                           parameters)
      _ <- err.either(invokeEither)
    } yield ()
    
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
}

object ToolProgram {
  def apply[F[_]](implicit M: components.Model[F]): ToolProgram[F] = new ToolProgram[F] {
    val model: components.Model[F] = M
  }
}
