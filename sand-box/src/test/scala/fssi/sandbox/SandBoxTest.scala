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
    val version    = "8"
    val projectDir = Paths.get(project)
    val outputFile = Paths.get(output).toFile
    sandBox.compileContract(projectDir, version, outputFile) match {
      case Right(_) => info(s"compile contract success,checkout $output")
      case Left(e)  => e.printStackTrace()
    }
  }

  test("check contract determinism") {
    val output     = "/Users/songwenchao/Documents/source/self/pratice/fssi_test/out"
    val outputFile = Paths.get(output).toFile
    sandBox.checkContractDeterminism(outputFile) match {
      case Right(_) => info("check passed")
      case Left(e)  => e.printStackTrace()
    }
  }

  test("run smart contract") {
    val output     = "/Users/songwenchao/Documents/source/self/pratice/fssi_test/out"
    val outputFile = Paths.get(output).toFile
    val context    = new TestContext
    val methodName = "function0"
    val parameter  = PEmpty
//    val parameter = PArray(PString("haha"), PBigDecimal(123))
    sandBox.executeContract(context, outputFile, Method(methodName), parameter) match {
      case Right(_) => info("run finished")
      case Left(e)  => e.printStackTrace()
    }
  }
}
