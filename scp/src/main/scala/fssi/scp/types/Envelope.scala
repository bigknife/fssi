package fssi.scp.types

case class Envelope[M <: Message](
  statement: Statement[M],
  signature: Signature
)
