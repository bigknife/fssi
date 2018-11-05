package fssi.scp

import types._
import scala.collection.immutable._

case class TestValue(values: TreeSet[Int]) extends Value {

  def rawBytes: Array[Byte] = {
    def _toB(i: Int): Array[Byte] = BigInt(i).toByteArray
    values.toVector.map(_toB).foldLeft(Array.emptyByteArray)(_ ++ _)
  }

  def compare(other: Value): Int = other match {
    case TestValue(otherValues) =>
      values.mkString("-").compareTo(otherValues.mkString("-"))
    case _ => throw new RuntimeException("only TestValue is allowed")
  }
}
