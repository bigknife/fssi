package fssi.consensus.scp.ast.domain.types

/** a ballot for some value
  * a ballot is a pair of b=(n,x), x is a value to externalized for some slot, and n is the referendum on externalizing
  * x for the slot.
  */
case class Ballot(
    counter: Int, // n
    value: Value // x
)
