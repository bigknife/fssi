package fssi
package types

trait JsonMessageHandler {
  def ignored(message: JsonMessage): Boolean
  def handle(message: JsonMessage): Unit
}
