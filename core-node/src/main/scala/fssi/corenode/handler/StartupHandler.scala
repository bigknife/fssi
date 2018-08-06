package fssi
package corenode
package handler

import types._
import interpreter._
import types.syntax._
import ast._, uc._

import io.circe._
import io.circe.syntax._
import json.implicits._

trait StartupHandler {
  val coreNodeProgram = CoreNodeProgram[components.Model.Op]

  def apply(setting: Setting.CoreNodeSetting): Unit = {
    val node = runner.runIO(coreNodeProgram.startup(handleJsonMessage), setting).unsafeRunSync
    println(showNode(node))
  }

  def showNode(node: Node): String = node.toString

  def handleJsonMessage(jsonMessage: JsonMessage): Unit = {
    println(s"Got JsonMessage: $jsonMessage")
  }


}
