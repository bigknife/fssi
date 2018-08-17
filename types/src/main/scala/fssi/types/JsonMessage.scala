package fssi
package types

/** Json message is a simple json string message, which
  * includes a real message body and the data type.
  */
case class JsonMessage(typeName: String, body: String)

object JsonMessage {
  /** Transaction Json Message
    */
  val TYPE_NAME_TRANSACTION = "transaction"
}
