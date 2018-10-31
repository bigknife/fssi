package fssi.scp.types

case class Envelope[M <: Message](
    statement: Statement[M],
    signature: Signature
) {
  def to[N <: Message]: Envelope[N] = this.asInstanceOf[Envelope[N]]
}

object Envelope {

  trait Implicits {
    import fssi.base.implicits._
    import fssi.scp.types.implicits._

    implicit def envelopeToBytes[M <: Message](envelope: Envelope[M]): Array[Byte] =
      envelope.statement.asBytesValue.bytes ++ envelope.signature.asBytesValue.bytes
  }
}
