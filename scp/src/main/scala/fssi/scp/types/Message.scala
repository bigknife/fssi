package fssi.scp.types

sealed trait Message

object Message {

  def bunch(xs: Message*): Bunches = Bunches(xs.toVector)
  def bunchOption(xs: Option[Message]*): Bunches = Bunches(xs.flatMap(_.toVector).toVector)

  // vote(nominate x)
  case class VoteNominations(values: ValueSet) extends Message

  // vote(accept(nominate x))
  case class AcceptNominations(values: ValueSet) extends Message {
    def asVote: VoteNominations = VoteNominations(values)
  }

  // vote(prepare b)
  case class VotePrepare(ballot: Ballot, prepared: Ballot, preparedPrime: Ballot) extends Message

  // vote(accept(prepare b))
  case class AcceptPrepare(ballot: Ballot, prepared: Ballot, preparedPrime: Ballot) extends Message

  // vote(commit b)
  case class VoteCommit(ballot: Ballot, lowestCounter: Int, highestCounter: Int) extends Message

  // vote(accept(commit b))
  case class AcceptCommit(ballot: Ballot) extends Message

  // externalize(b)
  case class Externalize(ballot: Ballot) extends Message

  // combined messages
  case class Bunches(messages: Vector[Message]) extends Message {
    def isEmpty: Boolean = messages.isEmpty
    def nonEmpty: Boolean = messages.nonEmpty
  }
}
