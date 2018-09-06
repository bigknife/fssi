package fssi
package sandbox
import java.io.File
import java.nio.file.Path

import fssi.types.exception.{ContractCheckException, ContractCompileException}

class SandBox {

  private lazy val compiler = new fssi.sandbox.world.Compiler
  private lazy val checker  = new fssi.sandbox.world.Checker

  def compileContract(rootPath: Path,
                      version: String,
                      outputFile: File): Either[ContractCompileException, Unit] =
    compiler.compileContract(rootPath, version, outputFile)

  def checkContractDeterminism(contractFile: File): Either[ContractCheckException, Unit] =
    checker.checkDeterminism(contractFile)
}
