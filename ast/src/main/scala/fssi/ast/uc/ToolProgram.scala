package fssi
package ast
package uc

import types._
import types.syntax._
import bigknife.sop._
import bigknife.sop.implicits._
import java.io.File
import java.nio.file.Path

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
      createRoot   <- chainStore.createChainRoot(dataDir, chainID)
      root         <- err.either(createRoot)
      _            <- blockStore.initialize(root)
      _            <- tokenStore.initialize(root)
      _            <- contractStore.initialize(root)
      _            <- contractDataStore.initialize(root)
      genesisBlock <- blockService.createGenesisBlock(chainID)
      _            <- blockStore.saveBlock(genesisBlock)
    } yield ()
  }

  /***
    * compile smart contract
    * @param sourceDir path to read contract source code
    * @param destDir path to store contract zip
    */
  def compileContract(sourceDir: Path, destDir: Path, format: CodeFormat): SP[F, Unit] = {
    for {
      classPathEither   ← contractService.compileContractSourceCode(sourceDir)
      classPath         ← err.either(classPathEither)
      determinismEither ← contractService.checkDeterministicOfClass(classPath)
      _                 ← err.either(determinismEither)
      bytesValue        ← contractService.zipContract(classPath)
      _                 ← contractService.outputZipFile(bytesValue, destDir, format)
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
                  parameters: Array[String],
                  decodeFormat: CodeFormat): SP[F, Unit] = {
    for {
      codeBytes   ← contractService.decodeContractClasses(classesDir, decodeFormat)
      contractDir ← contractService.buildContractDir(codeBytes)
      checkEither ← contractService.checkContractMethod(contractDir, clazzName, methodName)
      _           ← err.either(checkEither)
      invokeEither ← contractService.invokeContractMethod(contractDir,
                                                          clazzName,
                                                          methodName,
                                                          parameters)
      _ ← err.either(invokeEither)
    } yield ()
  }
}

object ToolProgram {
  def apply[F[_]](implicit M: components.Model[F]): ToolProgram[F] = new ToolProgram[F] {
    val model: components.Model[F] = M
  }
}
