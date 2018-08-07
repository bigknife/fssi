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

trait StartupHandler extends JsonMessageHandler{
  val coreNodeProgram = CoreNodeProgram[components.Model.Op]

  def apply(setting: Setting.CoreNodeSetting): Unit = {
    val node = runner.runIO(coreNodeProgram.startup(this), setting).unsafeRunSync

    // add shutdown hook to clean resources.
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      runner.runIO(coreNodeProgram.shutdown(node), setting).unsafeRunSync
    }))

    // long running.
    Thread.currentThread.join()
  }

  def showNode(node: Node): String = node.toString

  def ignored(message: JsonMessage): Boolean = {
    println(s"ignore message: $message")
    true
  }

  def handle(jsonMessage: JsonMessage): Unit = {
    println(s"Got JsonMessage: $jsonMessage")
  }

}
