package fssi.ast.domain.exceptions

case class LedgerStateError(currentHeight: BigInt, height: BigInt)
  extends FSSIException(s"Ledger states error, current is $currentHeight, try next is $height")
