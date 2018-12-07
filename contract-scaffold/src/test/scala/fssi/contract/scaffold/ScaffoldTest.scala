package fssi.contract.scaffold
import java.nio.file.Paths

import org.scalatest.FunSuite

class ScaffoldTest extends FunSuite {

  val scaffold = new ContractScaffold

  val projectRoot = "/tmp/fssi_scaffold"
  val projectPath = Paths.get(projectRoot)

  test("test create contract project") {
    scaffold.createContractProject(projectPath) match {
      case Right(_) => println("contract project created")
      case Left(e)  => e.printStackTrace()
    }
  }
}
