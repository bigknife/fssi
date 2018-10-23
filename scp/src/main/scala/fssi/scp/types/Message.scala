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
    def commitableBallot: Option[Ballot]
    def externalizableBallot: Option[Ballot]
  }

  case class Prepare(
      b: Ballot,
      p: Option[Ballot],
      `p'`: Option[Ballot],
      `c.n`: Int,
      `h.n`: Int
  ) extends BallotMessage {
    def workingBallot: Ballot = b
    def commitableBallot: Option[Ballot] =
      if(`c.n` != 0) Some(Ballot(`h.n`, b.value))
      else None

    def externalizableBallot: Option[Ballot] = None

  }

  case class Confirm(
      b: Ballot,
      `p.n`: Int,
      `c.n`: Int,
      `h.n`: Int
  ) extends BallotMessage {
    def workingBallot: Ballot = Ballot(`c.n`, b.value)
    def commitableBallot: Option[Ballot] = Some(Ballot(`h.n`, b.value))

    def externalizableBallot: Option[Ballot] = Some(Ballot(`h.n`, b.value))
  }

  case class Externalize(
      x: Value,
      `c.n`: Int,
      `h.n`: Int
  ) extends BallotMessage {
    def workingBallot: Ballot = Ballot(`c.n`, x)
    def commitableBallot: Option[Ballot] = Some(Ballot(`h.n`, x))

    def externalizableBallot: Option[Ballot] = Some(Ballot(`h.n`, x))
  }

}
