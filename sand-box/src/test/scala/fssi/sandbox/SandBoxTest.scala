package fssi
package sandbox

import java.nio.file.Paths

object SandBoxTest extends App {

  val compiler = new Compiler
//  val projectPath    = "/Users/songwenchao/Documents/source/self/pratice/fssi_test/fssi_study"
//  val outputFilePath = "/Users/songwenchao/Documents/source/self/pratice/fssi_test/out"
//  val outputFile     = Paths.get(outputFilePath).toFile
//  val rootPath       = Paths.get(projectPath)
//  compiler.compileContract(rootPath, "8", outputFile) match {
//    case Right(_) => println(s"compile project success,checkout file $outputFilePath")
//    case Left(e)  => e.printStackTrace()
//  }

  val targetPath   = "/Users/songwenchao/Documents/source/self/pratice/fssi_test/target"
  val contractPath = Paths.get(targetPath)
  compiler.checkDeterminism(contractPath) match {
    case Right(_) => println("smart contract determinism checked passed")
    case Left(e)  => e.printStackTrace()
  }
}
