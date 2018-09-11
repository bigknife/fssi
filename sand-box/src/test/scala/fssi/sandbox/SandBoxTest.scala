package fssi
package sandbox
import java.nio.file.Paths

import fssi.types.Contract.Method
import fssi.types.Contract.Parameter.{PArray, PBigDecimal, PEmpty, PString}
import org.scalatest.FunSuite

class SandBoxTest extends FunSuite {

  val sandBox = new SandBox

  test("test compile contract") {
    val project    = "/Users/songwenchao/Documents/source/self/pratice/fssi_test/fssi_study"
    val output     = "/Users/songwenchao/Documents/source/self/pratice/fssi_test/out"
    val version    = "0.0.1"
    val projectDir = Paths.get(project)
    val outputFile = Paths.get(output).toFile
    sandBox.compileContract(projectDir, version, outputFile) match {
      case Left(e)      => e.printStackTrace()
      case Right(value) =>
    }
  }

  test("check contract determinism") {
    val output     = "/Users/songwenchao/Documents/source/self/pratice/fssi_test/out"
    val outputFile = Paths.get(output).toFile
    sandBox.checkContractDeterminism(outputFile) match {
      case Left(e)      => e.printStackTrace()
      case Right(value) =>
    }
  }

  test("run smart contract") {
    val output     = "/Users/songwenchao/Documents/source/self/pratice/fssi_test/out"
    val outputFile = Paths.get(output).toFile
    val context    = new TestContext
    val methodName = "function3"
    val parameter  = PArray(PString("abcdefg"), PBigDecimal(1))
    sandBox.executeContract(context, outputFile, Method(methodName), parameter) match {
      case Left(e)      => e.printStackTrace()
      case Right(value) =>
    }
  }
}
