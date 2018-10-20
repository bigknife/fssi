package fssi.scp

package object types {
  type ValueSet = scala.collection.immutable.TreeSet[Value]
  object ValueSet {
    def empty: ValueSet = scala.collection.immutable.TreeSet.empty[Value]
    def apply(v: Value*): ValueSet = scala.collection.immutable.TreeSet(v: _*)
  }

  type BallotSet = scala.collection.immutable.TreeSet[Ballot]
  object BallotSet {
    def empty: BallotSet = scala.collection.immutable.TreeSet.empty[Ballot]
    def apply(v: Ballot*): BallotSet = scala.collection.immutable.TreeSet(v: _*)
  }

  type StateChanged = Boolean

  type CounterSet = scala.collection.immutable.TreeSet[Int]
}
