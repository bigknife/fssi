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

import bigknife.jsonrpc._, Request.implicits._

import java.io._

trait CreatePublishContractTransactionHandler {
  def apply(accountFile: File, password: Array[Byte], contractFile: File): Unit = {
    ???
  }
}
