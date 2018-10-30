package fssi
package tool
package handler

import types._
import interpreter._
import ast._, uc._

import io.circe._
import io.circe.syntax._
import json.implicits._
import java.io._

trait CompileContractToolProgram extends BaseToolProgram {
  def apply(accountFile: File,
            secretKeyFile: File,
            projectDirectory: File,
            outputFile: File,
            sandboxVersion: String): Effect = {

    for {
      _ <- toolProgram.compileContract(accountFile,
                                       secretKeyFile,
                                       projectDirectory,
                                       outputFile,
                                       sandboxVersion)
      _ <- logger.info("contract compiled")
    } yield ()
  }
}
