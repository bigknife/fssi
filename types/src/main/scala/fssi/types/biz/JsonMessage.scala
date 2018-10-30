package fssi
package types
package biz

/** Json message is a simple json string message, which
  * includes a real message body and the data type.
  */
case class JsonMessage(typeName: String, body: String)

object JsonMessage {
  /** Transaction Json Message
    */
  val TYPE_NAME_TRANSACTION = "transaction"

  /** SCP Envelope Message 
    */
  val TYPE_NAME_SCP = "scpEnvelope"

  /* SCP QuorumSet Sync Message
   */
  val TYPE_NAME_QUORUMSET_SYNC = "qsSync"

  /** create a scp json message */
  def scpEnvelopeJsonMessage(body: String): JsonMessage = JsonMessage(TYPE_NAME_SCP, body)

  /** create a scp qs sync message
    */
  def scpQsSyncJsonMessage(body: String): JsonMessage = JsonMessage(TYPE_NAME_QUORUMSET_SYNC, body)
}
