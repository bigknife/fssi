package fssi.scp

import types._
import scala.collection.immutable._

case class TestValue(values: TreeSet[Int]) extends Value {
  def compare(other: Value): Int = other match {
    case TestValue(otherValues) =>
      Ordering[Int].compare(values.size, otherValues.size)
    case _ => throw new RuntimeException("only TestValue is allowed")
  }
}
