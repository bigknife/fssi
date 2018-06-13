package fssi.consensus.scp.ast.domain.types

/** a statement to be validate */
sealed trait Statement {
  def nodeID: Node.ID // v
  def slotIndex: Long // i
  def quorumSetHash: Hash // D specifies Q(v)
}

object Statement {

  sealed trait NominationStatement extends Statement
  sealed trait BallotStatement extends Statement

  case class Prepare(
      nodeID: Node.ID, // node v
      slotIndex: Long, // index this statement is about
      quorumSetHash: Hash, //D specifies Q(v)
      ballot: Ballot, // b current ballot that node v is attempting to prepare and commit (b =/ 0)
      prepared: Ballot, // p
      preparedPrime: Ballot, // p'
      nC: Long, // n(slotIndex) of the lowest ballot
      nH: Long // n(slotIndex) of the highest ballot
  ) extends BallotStatement

  case class Confirm(
      nodeID: Node.ID, // node v
      slotIndex: Long, // index this statement is about
      quorumSetHash: Hash, //D specifies Q(v)
      ballot: Ballot, // b
      nPrepared: Long, // p.n
      nCommit: Long, // c.n
      nH: Long // h.n
  ) extends BallotStatement

  case class Externalize(
      nodeID: Node.ID, // node v
      slotIndex: Long, // index this statement is about
      quorumSetHash: Hash, //D specifies Q(v)
      commit: Ballot, // c
      nH: Long // h.n
  ) extends BallotStatement

  case class Nominate(
      nodeID: Node.ID, // node v
      slotIndex: Long, // index this statement is about
      quorumSetHash: Hash, //D specifies Q(v)
      votes: Set[Value],
      accepted: Set[Value]
  ) extends NominationStatement

  // timestamped historical statement wrapper
  case class HistoricalStatement(
      statement: Statement,
      timestamp: Long,
      validated: Boolean
  )

  // predict
  type Predict = Statement => Boolean
  def predict(p: Statement => Boolean): Predict = p
}
