package fssi
package types
package biz

trait JsonMessageHandler {
  def ignored(message: JsonMessage): Boolean
  def handle(message: JsonMessage): Unit
}
