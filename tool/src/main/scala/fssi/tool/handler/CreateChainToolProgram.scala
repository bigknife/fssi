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
import org.slf4j._

trait CreateChainToolProgram extends BaseToolProgram {
  def apply(rootDir: File, chainId: String): Effect = {
    for {
      _ <- toolProgram.createChain(rootDir, chainId)
      _ <- logger.info("created")
    } yield ()
  }
}
