package fssi.scp

package object types {
  type ValueSet = scala.collection.immutable.TreeSet[Value]
  object ValueSet {
    def empty: ValueSet = scala.collection.immutable.TreeSet.empty[Value]
    def apply(v: Value*): ValueSet = scala.collection.immutable.TreeSet(v: _*)
  }
}
