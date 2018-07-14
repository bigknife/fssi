package fssi.contract

import _root_.java.util
import _root_.scala.collection.JavaConverters._

case class States(states: Map[String, AccountState]) {
  def of(accountId: String): Option[AccountState] = states.get(accountId)
  def update(accountState: AccountState): States = States(states + (accountState.accountId -> accountState))

  lazy val bytes: Array[Byte] = {
    states.toVector.sortBy(_._1).foldLeft(Array.emptyByteArray) {(acc, n) =>
      acc ++ n._1.getBytes("utf-8") ++ n._2.bytes
    }
  }
}

object States {
  def apply(jstates: Seq[AccountState]): States = States(jstates.map(x => x.accountId -> x).toMap)
  def apply(jstates: util.List[AccountState]): States = apply(jstates.asScala)
}
