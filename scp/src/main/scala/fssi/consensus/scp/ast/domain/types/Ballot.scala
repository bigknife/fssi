package fssi.consensus.scp.ast.domain.types

/** a ballot for some value
  * a ballot is a pair of b=(n,x), x is a value to externalized for some slot, and b is the referendum on externalizing
  * n (n >= 1) is a counter to ensure higher ballot numbers are always available
  * x for the slot.
  */
case class Ballot(
    counter: Int, // n
    value: Value // x
)

object Ballot {
  val Empty: Ballot = Ballot(0, Value.Empty)

  sealed trait Phase
  object Phase {
    case object Prepare extends Phase {
      override def toString: String = "Prepare"
    }
    case object Confirm extends Phase {
      override def toString: String = "Confirm"
    }
    case object Externalize extends Phase {
      override def toString: String = "Externalize"
    }
  }

  def preparePhrase: Phase = Phase.Prepare
  def confirmPhrase: Phase = Phase.Confirm
  def externalizePhrase: Phase = Phase.Externalize

  case class State(
      currentBallot: Ballot, //b
      prepared: Ballot, //p
      preparedPrime: Ballot, //p'
      highBallot: Ballot, //h
      commit: Ballot, // c
      latestEnvelopes: Map[Node.ID, Envelope], //M
      phase: Phase //phrase i
  )
}
