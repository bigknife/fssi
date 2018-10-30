package fssi
package types
package biz

/** all kinds of network message
  */
sealed trait Message

object Message {
  case class ConsensusMessage(payload: Array[Byte]) extends Message
  case class ApplicationMessage(payload: Array[Byte]) extends Message
  case class ClientMessage(payload: Array[Byte]) extends Message

  def handler[A <: Message](fun: A => Unit): Handler[A] = Handler(fun)

  sealed trait Handler[A <: Message] {
    protected val fun: A => Unit

    def apply(message: A): Unit = fun(message)
  }

  object Handler {
    def apply[A <: Message](_fun: A => Unit): Handler[A] = new Handler[A] {
      protected val fun = _fun
    }
  }

}
