package fssi.scp.types

case class Envelope[M <: Message](
  statement: Statement[M],
  signature: Signature
) {
  def to[N <: Message]: Envelope[N] = this.asInstanceOf[Envelope[N]]
}
