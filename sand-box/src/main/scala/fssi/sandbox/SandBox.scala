package fssi
package sandbox
import java.io.File
import java.nio.file.Path

import fssi.contract.lib.Context
import fssi.sandbox.world._
import fssi.types.biz._
import fssi.types.exception.FSSIException
import fssi.utils.FileUtil

class SandBox {

  private lazy val compiler = new Compiler
  private lazy val checker  = new Checker
  private lazy val builder  = new Builder
  private lazy val runner   = new Runner

  def compileContract(accountId: Account.ID,
                      publicKey: Account.PubKey,
                      privateKey: Account.PrivKey,
                      rootPath: Path,
                      version: String,
                      outputFile: File): Either[FSSIException, Unit] =
    compiler.compileContract(accountId.value,
                             publicKey.value,
                             privateKey.value,
                             rootPath,
                             version,
                             outputFile)

  def checkContractDeterminism(publicKey: Account.PubKey,
                               contractFile: File): Either[FSSIException, Unit] = {
    for {
      contractBytes <- builder.readContractBytesFromFile(publicKey.value, contractFile)
      contractPath <- builder.buildContractProjectFromBytes(contractBytes,
                                                            contractFile.getParentFile.toPath)
      _ <- checker.checkDeterminism(contractPath)
    } yield FileUtil.deleteDir(contractPath)
  }

  def buildUnsignedContract(publicKey: Account.PubKey,
                            file: File): Either[FSSIException, Contract.UserContract] = {
    for {
      contractBytes <- builder.readContractBytesFromFile(publicKey.value, file)
      contractPath <- builder.buildContractProjectFromBytes(contractBytes,
                                                            file.getParentFile.toPath)
      contract <- builder.buildUserContractFromPath(contractPath, contractBytes)
    } yield { FileUtil.deleteDir(contractPath); contract }
  }

  def executeContract(publicKey: Account.PubKey,
                      context: Context,
                      contractFile: File,
                      method: Contract.UserContract.Method,
                      params: Contract.UserContract.Parameter): Either[FSSIException, Unit] = {
    for {
      contractBytes <- builder.readContractBytesFromFile(publicKey.value, contractFile)
      contractPath <- builder.buildContractProjectFromBytes(contractBytes,
                                                            contractFile.getParentFile.toPath)
      methodMeta <- builder.buildContractMeta(contractPath)
      methods    <- checker.checkContractDescriptor(methodMeta.interfaces)
      _          <- checker.isContractMethodExposed(method, params, methods)
      _          <- checker.checkDeterminism(contractPath)
      contractMethod = methods.find(_.alias == method.alias).get
      _ <- runner.invokeContractMethod(context, contractPath, contractMethod, params)
    } yield FileUtil.deleteDir(contractPath)
  }

  def checkRunningEnvironment: Either[FSSIException, Unit] =
    checker.isSandBoxEnvironmentValid
}
