package fssi.store

import java.io._

import jetbrains.exodus.ArrayByteIterable
import jetbrains.exodus.env._

class XodusKVStore(storeName: String, environment: Environment, transaction: Option[Transaction])
    extends KVStore {

  override def get(key: Array[Byte]): Option[Array[Byte]] = {
    val txn = transaction.getOrElse(environment.beginReadonlyTransaction())
    def block: Option[Array[Byte]] = {
      val store = environment.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, txn)
      Option(store.get(txn, new ArrayByteIterable(key))).map(_.getBytesUnsafe)
    }
    val r = block
    if (transaction.isEmpty) txn.abort()
    r
  }

  override def put(key: Array[Byte], value: Array[Byte]): Either[Throwable, Boolean] = {
    val txn = transaction.getOrElse(environment.beginTransaction())
    def block(): Boolean = {
      val store = environment.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, txn)
      store.put(txn, new ArrayByteIterable(key), new ArrayByteIterable(value))
    }
    if (transaction.isEmpty) {
      runInTransaction(txn){
        block()
      }

    } else {
      scala.util.Try {
        block()
      }.toEither
    }
  }

  override def delete(key: Array[Byte]): Either[Throwable, Boolean] = {
    val txn = transaction.getOrElse(environment.beginTransaction())
    def block(): Boolean = {
      val store = environment.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, txn)
      store.delete(txn, new ArrayByteIterable(key))
    }
    if (transaction.isEmpty) {
      runInTransaction(txn){
        block()
      }
    } else {
      scala.util.Try {
        block()
      }.toEither
    }
  }

  override def keyIterator: Iterator[Array[Byte]] = {
    val txn = transaction.getOrElse(environment.beginTransaction())
    val store = environment.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, txn)
    val cursor = store.openCursor(txn)
    new Iterator[Array[Byte]] {
      override def hasNext: Boolean = cursor.getNext
      override def next(): Array[Byte] = cursor.getKey.getBytesUnsafe
    }
  }

  private def runInTransaction[A](txn: Transaction)(block:  => A): Either[Throwable, A] = {
    def _loop(): A = {
      val r: A = block()
      if (txn.flush()) r
      else {
        txn.revert()
        _loop()
      }
    }

    scala.util.Try {
      val res = _loop()
      val r = txn.commit()
      if (!r) throw new RuntimeException("transaction not flushed, nothing committed")
      else res
    }.recover {
      case x: Throwable =>
        txn.abort()
        throw x
    }.toEither
  }

}
