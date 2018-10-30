package fssi.corenode

import fssi.types.biz.{JsonMessage, JsonMessageHandler, Transaction}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import fssi.interpreter._
import fssi.ast.{Effect, _}
import fssi.types.json.implicits._
import fssi.scp.types.{Envelope, Message}
import org.slf4j.LoggerFactory

trait CoreNodeJsonMessageHandler extends JsonMessageHandler{

  private val log = LoggerFactory.getLogger(getClass)
  def setting: Setting

  private val acceptedTypeNames: Vector[String] = Vector(
    JsonMessage.TYPE_NAME_TRANSACTION,
    JsonMessage.TYPE_NAME_QUORUMSET_SYNC,
    JsonMessage.TYPE_NAME_SCP
  )
  override  def ignored(message: JsonMessage): Boolean = !acceptedTypeNames.contains(message.typeName)

  override def handle(message: JsonMessage): Unit = {
    import JsonMessage._
    val p: Either[Exception, Effect] = message.typeName match {
      case TYPE_NAME_TRANSACTION =>
        for {
          json <- parse(message.body)
          transaction <- json.as[Transaction]
          effect <- handleTransaction(transaction)
        } yield effect
      case TYPE_NAME_SCP =>
        for {
          json <- parse(message.body)
          envelope <- json.as[Envelope[Message]]
          effect <- handleScpEnvelope(envelope)
        } yield effect
      case _ => Effect.empty
    }
    p match {
      case Left(e) =>
        log.error("parse message failed", e)
      case Right(effect) =>
        runner.runIOAttempt(effect, setting).unsafeRunSync() match {
          case Left(e) =>
            log.error("handle message failed", e)
          case Right(_) =>
            log.debug("handled message")
        }
    }
  }

  def handleTransaction(transaction: Transaction): Effect
  def handleScpEnvelope(envelope: Envelope[Message]): Effect

  implicit def toEitherError[A](a: => A): Either[Exception, A] = try {
    Right(a)
  } catch {
    case e: Exception => Left(e)
  }
}
