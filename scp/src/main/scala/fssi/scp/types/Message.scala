package fssi.scp.types

sealed trait Message

object Message {
  // vote(nominate x)
  case class VoteNominations(values: ValueSet) extends Message

  // vote(accept(nominate x))
  case class AcceptNominations(values: ValueSet) extends Message

  // vote(prepare b)
  case class VotePrepare(ballot: Ballot) extends Message

  // vote(accept(prepare b))
  case class AcceptPrepare(ballot: Ballot) extends Message

  // vote(commit b)
  case class VoteCommit(ballot: Ballot) extends Message

  // vote(accept(commit b))
  case class AcceptCommit(ballot: Ballot) extends Message

  // externalize(b)
  case class Externalize(ballot: Ballot) extends Message
}
