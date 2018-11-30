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
      _ <- checker.checkDeterminism(contractPath)(contractPath)(true)
    } yield ()
  }

  def buildUnsignedContract(publicKey: Account.PubKey,
                            file: File): Either[FSSIException, Contract.UserContract] = {
    for {
      contractBytes <- builder.readContractBytesFromFile(publicKey.value, file)
      contractPath <- builder.buildContractProjectFromBytes(contractBytes,
                                                            file.getParentFile.toPath)
      contract <- builder.buildUserContractFromPath(contractPath, contractBytes)(contractPath)(true)
    } yield contract
  }

  def executeContract(
      context: Context,
      contractCode: Contract.UserContract.Code,
      method: Contract.UserContract.Method,
      params: Option[Contract.UserContract.Parameter]): Either[FSSIException, Unit] = {
    for {
      tmpPath      <- builder.createDefaultContractTmpPath
      contractPath <- builder.buildContractProjectFromBytes(contractCode.value, tmpPath)
      methodMeta   <- builder.buildContractMeta(contractPath)(tmpPath)(false)
      methods      <- checker.checkContractDescriptor(methodMeta.interfaces)(tmpPath)(false)
      _            <- checker.isContractMethodExposed(method, params, methods)(tmpPath)(false)
      _            <- checker.checkDeterminism(contractPath)(tmpPath)(false)
      contractMethod = methods.find(_.alias == method.alias).get
      _ <- runner.invokeContractMethod(context, contractPath, contractMethod, params)(tmpPath)(true)
    } yield ()
  }

  def checkRunningEnvironment: Either[FSSIException, Unit] =
    checker.isSandBoxEnvironmentValid

  implicit def cleanCacheFile[A <: Any](
      either: Either[FSSIException, A]): Path => Boolean => Either[FSSIException, A] =
    path =>
      delete => {
        either.left
          .map(_ => FileUtil.deleteDir(path))
          .right
          .map(_ => if (delete) FileUtil.deleteDir(path))
        either
    }
}
