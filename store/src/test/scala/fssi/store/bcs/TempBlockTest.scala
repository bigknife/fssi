package fssi.store.bcs

import fssi.store.bcs.types.BCSKey

object TempBlockTest extends App {
  val bcs = BCS("/Users/benny/tmp/node_1/db")

  bcs.getPersistedMeta(BCSKey.MetaKey.ChainID) match {
    case Left(t) => t.printStackTrace()
    case Right(Some(d)) => println(new String(d.bytes))
    case Right(_) => println("no data")
  }

  bcs.getPersistedMeta(BCSKey.MetaKey.Height) match {
    case Left(t) => t.printStackTrace()
    case Right(Some(d)) => println(BigInt(d.bytes))
    case Right(_) => println("no data")
  }

  bcs.getPersistedMeta(BCSKey.MetaKey.Version) match {
    case Left(t) => t.printStackTrace()
    case Right(Some(d)) => println(new String(d.bytes))
    case Right(_) => println("no data")
  }

  bcs.close()
}
