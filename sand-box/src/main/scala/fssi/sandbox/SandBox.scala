package fssi
package sandbox
import java.io.File
import java.nio.file.Path

import fssi.contract.lib.Context
import fssi.sandbox.exception._
import fssi.sandbox.world._
import fssi.types.biz._

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
                      outputFile: File): Either[ContractCompileException, Unit] =
    compiler.compileContract(rootPath, version, outputFile)

  def checkContractDeterminism(contractFile: File): Either[ContractCheckException, Unit] =
    checker.checkDeterminism(contractFile)

  def buildContract(file: File): Either[ContractBuildException, Contract.UserContract] =
    builder.buildUserContractFromFile(file)

  def executeContract(
      context: Context,
      contractFile: File,
      method: Contract.UserContract.Method,
      params: Contract.UserContract.Parameter): Either[ContractRunningException, Unit] = {
    for {
      methodMeta <- builder
        .buildContractMeta(contractFile)
        .left
        .map(x => ContractRunningException(x.messages))
      methods <- checker
        .checkContractDescriptor(methodMeta.interfaces)
        .left
        .map(x => ContractRunningException(x.messages))
      _ <- checker
        .isContractMethodExisted(method, params, methods)
        .left
        .map(x => ContractRunningException(x.messages))
      _ <- checker
        .checkDeterminism(contractFile)
        .left
        .map(x => ContractRunningException(x.messages))
      contractMethod = methods.find(_.alias == method.alias).get
      _ <- runner
        .invokeContractMethod(context, contractFile, contractMethod, params)
    } yield ()
  }

  def checkRunningEnvironment: Either[SandBoxEnvironmentException, Unit] =
    checker.isSandBoxEnvironmentValid
}
