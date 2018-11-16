package fssi
package types
package biz

/** all kinds of network message
  */
sealed trait Message

object Message {
  trait ConsensusMessage extends Message

  sealed trait ApplicationMessage extends Message
  object ApplicationMessage {
    case class TransactionMessage(payload: Array[Byte]) extends ApplicationMessage
    case class QueryMessage(payload: Array[Byte])       extends ApplicationMessage
  }

  sealed trait ClientMessage extends Message
  object ClientMessage {
    case class SendTransaction(payload: Array[Byte])  extends ClientMessage
    case class QueryTransaction(payload: Array[Byte]) extends ClientMessage
  }

  def handler[A <: Message, R](fun: A => R): Handler[A, R] = Handler(fun)

  sealed trait Handler[A <: Message, R] {
    protected val fun: A => R

    def apply(message: A): R = fun(message)
  }

  object Handler {
    def apply[A <: Message, R](_fun: A => R): Handler[A, R] = new Handler[A, R] {
      protected val fun = _fun
    }
  }

}
