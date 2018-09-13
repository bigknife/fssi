package fssi
package tool
package handler

import types._
import interpreter._
import types.syntax._
import ast._, uc._

import io.circe._
import io.circe.syntax._
import json.implicits._

import java.io._
import org.slf4j._

trait BaseHandler {
  lazy val logger = LoggerFactory.getLogger(getClass)
  val toolProgram = ToolProgram[components.Model.Op]
  
}
