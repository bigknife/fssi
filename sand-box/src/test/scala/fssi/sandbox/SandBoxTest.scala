package fssi
package sandbox

import java.nio.file.Paths

import fssi.types.{Account, HexString, UniqueName, Version}

object SandBoxTest extends App {

  val sandbox        = new SandBox
  val projectPath    = "/Users/songwenchao/Documents/source/self/pratice/fssi_test/fssi_study"
  val outputFilePath = "/Users/songwenchao/Documents/source/self/pratice/fssi_test/out"
  val outputFile     = Paths.get(outputFilePath).toFile
  val rootPath       = Paths.get(projectPath)
  sandbox.compileContract(rootPath, "8", outputFile) match {
    case Right(_) => println(s"compile project success,checkout file $outputFilePath")
    case Left(e)  => e.printStackTrace()
  }
  sandbox.checkContractDeterminism(outputFile) match {
    case Right(_) => println("smart contract determinism checked passed")
    case Left(e)  => e.printStackTrace()
  }

  val accountId = Account.ID(HexString("1234567".getBytes("utf-8")))
  val name      = UniqueName("fssi-study")
  val version   = Version("8")
  sandbox.buildContract(accountId, outputFile, name, version) match {
    case Right(contract) => println(contract)
    case Left(e)         => e.printStackTrace()
  }
}
