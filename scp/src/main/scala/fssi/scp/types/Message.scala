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

    def isNewerThan(other: Nomination): Boolean =
      (other.voted subsetOf voted) && (other.voted.size < voted.size) ||
        (other.accepted subsetOf accepted) && other.accepted.size < accepted.size

  }

  sealed trait BallotMessage extends Message {
    def workingBallot: Ballot
    def commitableBallot: Option[Ballot]
    def externalizableBallot: Option[Ballot]
  }

  case class Prepare(
      b: Ballot,
      p: Option[Ballot] = None,
      `p'`: Option[Ballot] = None,
      `c.n`: Int = 0,
      `h.n`: Int = 0
  ) extends BallotMessage {
    def workingBallot: Ballot = b
    def commitableBallot: Option[Ballot] =
      if (`c.n` != 0) Some(Ballot(`h.n`, b.value))
      else None

    def externalizableBallot: Option[Ballot] = None

  }

  case class Confirm(
      b: Ballot,
      `p.n`: Int,
      `c.n`: Int,
      `h.n`: Int
  ) extends BallotMessage {
    def workingBallot: Ballot            = Ballot(`c.n`, b.value)
    def commitableBallot: Option[Ballot] = Some(Ballot(`h.n`, b.value))

    def externalizableBallot: Option[Ballot] = Some(Ballot(`h.n`, b.value))
  }

  case class Externalize(
      x: Value,
      `c.n`: Int,
      `h.n`: Int
  ) extends BallotMessage {
    def workingBallot: Ballot            = Ballot(`c.n`, x)
    def commitableBallot: Option[Ballot] = Some(Ballot(`c.n`, x))

    def externalizableBallot: Option[Ballot] = Some(Ballot(`c.n`, x))
  }

  def prepare(b: Ballot): Prepare = Prepare(b)

  def prepare(b: Ballot, p: Ballot): Prepare = Prepare(b, Some(p))

  def prepare(b: Ballot, p: Ballot, `p'`: Ballot): Prepare = Prepare(b, Some(p), Some(`p'`))

  trait Implicits {
    import fssi.base.implicits._
    import fssi.scp.types.implicits._
    implicit def messageToBytes(message: Message): Array[Byte] = message match {
      case Message.Nomination(voted, accepted) =>
        voted.toArray.asBytesValue.bytes ++ accepted.toArray.asBytesValue.bytes

      case ballotMessage: BallotMessage =>
        ballotMessage match {
          case prepare: Prepare =>
            prepare.b.asBytesValue.bytes ++ prepare.p.asBytesValue.bytes ++ prepare.`p'`.asBytesValue.bytes ++ prepare.`c.n`.asBytesValue.bytes ++ prepare.`h.n`.asBytesValue.bytes
          case confirm: Confirm =>
            confirm.b.asBytesValue.bytes ++ confirm.`p.n`.asBytesValue.bytes ++ confirm.`c.n`.asBytesValue.bytes ++ confirm.`h.n`.asBytesValue.bytes
          case ext: Externalize =>
            ext.x.asBytesValue.bytes ++ ext.`c.n`.asBytesValue.bytes ++ ext.`h.n`.asBytesValue.bytes
        }
    }
  }

}
