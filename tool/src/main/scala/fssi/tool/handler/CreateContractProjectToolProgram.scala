package fssi
package tool
package handler

import types._
import interpreter._
import ast._, uc._

import java.io._

trait CreateContractProjectToolProgram extends BaseToolProgram {

  def apply(projectDirectory: File): Effect = {
    for {
      _ <- toolProgram.createContractProject(projectDirectory)
      _ <- logger.info(s"created,please checkout $projectDirectory")
    } yield ()

  }
}
