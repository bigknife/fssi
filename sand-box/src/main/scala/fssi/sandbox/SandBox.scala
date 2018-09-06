package fssi
package sandbox
import java.io.File
import java.nio.file.Path

import fssi.sandbox.exception.{
  ContractBuildException,
  ContractCheckException,
  ContractCompileException
}
import fssi.sandbox.world._
import fssi.types._

class SandBox {

  private lazy val compiler = new Compiler
  private lazy val checker  = new Checker
  private lazy val builder  = new Builder

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
}
