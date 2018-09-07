package fssi
package sandbox
import java.io.File
import java.nio.file.Path

import fssi.contract.lib.Context
import fssi.sandbox.exception.{
  ContractBuildException,
  ContractCheckException,
  ContractCompileException,
  ContractRunningException
}
import fssi.sandbox.types.Method
import fssi.sandbox.world._
import fssi.types._

class SandBox {

  private lazy val compiler = new Compiler
  private lazy val checker  = new Checker
  private lazy val builder  = new Builder
  private lazy val runner   = new Runner

  def compileContract(rootPath: Path,
                      version: String,
                      outputFile: File): Either[ContractCompileException, Unit] =
    compiler.compileContract(rootPath, version, outputFile)

  def checkContractDeterminism(contractFile: File): Either[ContractCheckException, Unit] =
    checker.checkDeterminism(contractFile)

  def buildContract(accountId: Account.ID,
                    file: File,
                    name: UniqueName,
                    version: Version): Either[ContractBuildException, Contract.UserContract] =
    builder.buildUserContractFromFile(accountId, file, name, version)

  def executeContract(context: Context,
                      contractFile: File,
                      method: Contract.Method,
                      params: Contract.Parameter): Either[ContractRunningException, Unit] = {
    for {
      methods <- builder
        .buildContractMethod(contractFile)
        .left
        .map(x => ContractRunningException(x.messages))
      _ <- checker
        .isContractMethodExisted(method, params, methods)
        .left
        .map(x => ContractRunningException(x.messages))
        .right
        .map(_ => Vector.empty[Method])
      _ <- checker
        .checkDeterminism(contractFile)
        .left
        .map(x => ContractRunningException(x.messages))
        .right
        .map(_ => Vector.empty[Method])
      contractMethod = methods.find(_.alias == method.alias).get
      _ <- runner
        .invokeContractMethod(context, contractFile, contractMethod, params)
        .right
        .map(_ => Vector.empty[Method])
    } yield ()
  }
}
