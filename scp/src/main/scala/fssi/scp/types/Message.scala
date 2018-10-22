package fssi.scp.types

sealed trait Message

object Message {

  /** nomination msg
    */
  case class Nomination(
      voted: ValueSet,
      accepted: ValueSet
  ) extends Message {
    def allValues: ValueSet = voted ++ accepted
  }

  sealed trait BallotMessage extends Message {
    def workingBallot: Ballot
  }

  case class Prepare(
      b: Ballot,
      p: Option[Ballot],
      `p'`: Option[Ballot],
      `c.n`: Int,
      `p.n`: Int
  ) extends BallotMessage {
    def workingBallot: Ballot = b
  }

  case class Confirm(
      b: Ballot,
      `p.n`: Int,
      `c.n`: Int,
      `h.n`: Int
  ) extends BallotMessage {
    def workingBallot: Ballot = Ballot(`c.n`, b.value)
  }

  case class Externalize(
      x: Value,
      `c.n`: Int,
      `h.n`: Int
  ) extends BallotMessage {
    def workingBallot: Ballot = Ballot(`c.n`, x)
  }

}
