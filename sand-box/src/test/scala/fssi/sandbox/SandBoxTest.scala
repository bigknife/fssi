package fssi
package sandbox
import java.nio.file.Paths

import fssi.types.Contract.Method
import fssi.types.Contract.Parameter.{PArray, PBigDecimal, PEmpty, PString}
import org.scalatest.FunSuite

class SandBoxTest extends FunSuite {

  val sandBox = new SandBox

  test("test compile contract") {
    val project    = "/Users/songwenchao/Documents/source/company/weihui/chain/fssi_contract"
    val output     = "/Users/songwenchao/opt/fssi/banana.contract"
    val version    = "1.0.0"
    val projectDir = Paths.get(project)
    val outputFile = Paths.get(output).toFile
    sandBox.compileContract(projectDir, version, outputFile)
  }

  test("check contract determinism") {
    val output     = "/Users/songwenchao/opt/fssi/banana.contract"
    val outputFile = Paths.get(output).toFile
    sandBox.checkContractDeterminism(outputFile)
  }

  test("run smart contract") {
    val output     = "/Users/songwenchao/opt/fssi/banana.contract"
    val outputFile = Paths.get(output).toFile
    val context    = new TestContext
    val methodName = "registerBanana"
    val parameter  = PArray(PString("Fee"), PBigDecimal(100))
    sandBox.executeContract(context, outputFile, Method(methodName), parameter)
  }
}
