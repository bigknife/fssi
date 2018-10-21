package fssi.scp.types

sealed trait Message

object Message {

  /** nomination msg
    */
  case class Nomination(
    voted: ValueSet,
    accepted: ValueSet
  ) extends Message

  sealed trait BallotMessage extends Message

  case class Prepare(
    b: Ballot,
    p: Ballot,
    `p'`: Ballot,
    `c.n`: Int,
    `p.n`: Int
  ) extends BallotMessage

  case class Confirm(
    b: Ballot,
    `p.n`: Int,
    `c.n`: Int,
    `h.n`: Int
  ) extends BallotMessage

  case class Externalize(
    x: Value,
    `c.n`: Int,
    `h.n`: Int
  ) extends BallotMessage

}
