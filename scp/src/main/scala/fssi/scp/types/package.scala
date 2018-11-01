package fssi.scp

package object types {
  type ValueSet = scala.collection.immutable.TreeSet[Value]
  object ValueSet {
    def empty: ValueSet            = scala.collection.immutable.TreeSet.empty[Value]
    def apply(v: Value*): ValueSet = scala.collection.immutable.TreeSet(v: _*)
  }

  type BallotSet = scala.collection.immutable.TreeSet[Ballot]
  object BallotSet {
    def empty: BallotSet             = scala.collection.immutable.TreeSet.empty[Ballot]
    def apply(v: Ballot*): BallotSet = scala.collection.immutable.TreeSet(v: _*)
  }

  type StateChanged = Boolean

  type CounterSet = scala.collection.immutable.TreeSet[Int]
  object CounterSet {
    def empty: CounterSet          = scala.collection.immutable.TreeSet.empty[Int]
    def apply(v: Int*): CounterSet = scala.collection.immutable.TreeSet(v: _*)
  }

  val BALLOT_TIMER   = "ballot_timer"
  val NOMINATE_TIMER = "nominate_timer"

  object implicits
      extends NodeID.Implicits
      with Signature.Implicits
      with SlotIndex.Implicits
      with Timestamp.Implicits
      with Value.Implicits
      with QuorumSet.Implicits
      with Ballot.Implicits
      with Message.Implicits
      with Statement.Implicits
      with Envelope.Implicits
}
