package fssi.store.core

import jetbrains.exodus.ArrayByteIterable
import jetbrains.exodus.env._
import org.slf4j.LoggerFactory

/**
  * a store which key can be route to different store.
  * then, we can get cross-store transaction ability
  */
abstract class RouteXodusKVStore(environment: Environment, transaction: Option[Transaction])
    extends KVStore {
  private val log = LoggerFactory.getLogger(getClass)

  def routeStoreName(key: Array[Byte]): String
  def routeStoreKey(key: Array[Byte]): Array[Byte]

  override def get(key: Array[Byte]): Option[Array[Byte]] = {
    val txn   = transaction.getOrElse(environment.beginReadonlyTransaction())
    val store = environment.openStore(routeStoreName(key), StoreConfig.WITHOUT_DUPLICATES, txn)
    def block: Option[Array[Byte]] = {
      Option(store.get(txn, new ArrayByteIterable(routeStoreKey(key)))).map(_.getBytesUnsafe)
    }
    val r = block
    if (transaction.isEmpty) txn.abort()
    r
  }

  override def put(key: Array[Byte], value: Array[Byte]): Either[Throwable, Boolean] = {
    val txn = transaction.getOrElse(environment.beginTransaction())
    def block(): Boolean = {
      val store = environment.openStore(routeStoreName(key), StoreConfig.WITHOUT_DUPLICATES, txn)
      store.put(txn, new ArrayByteIterable(routeStoreKey(key)), new ArrayByteIterable(value))
    }
    if (transaction.isEmpty) {
      runInTransaction(txn) {
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
      val store = environment.openStore(routeStoreName(key), StoreConfig.WITHOUT_DUPLICATES, txn)
      store.delete(txn, new ArrayByteIterable(routeStoreKey(key)))
    }
    if (transaction.isEmpty) {
      runInTransaction(txn) {
        block()
      }
    } else {
      scala.util.Try {
        block()
      }.toEither
    }
  }

  override def keyIterator: Iterator[Array[Byte]] = {
    val txn    = transaction.getOrElse(environment.beginTransaction())
    val store  = environment.openStore(routeStoreName(???), StoreConfig.WITHOUT_DUPLICATES, txn)
    val cursor = store.openCursor(txn)
    new Iterator[Array[Byte]] {
      override def hasNext: Boolean    = cursor.getNext
      override def next(): Array[Byte] = cursor.getKey.getBytesUnsafe
    }
  }

  private def runInTransaction[A](txn: Transaction)(block: => A): Either[Throwable, A] = {
    def _loop(): A = {
      val r: A    = block
      val flushed = txn.flush()
      if (flushed) r
      else {
        txn.revert()
        _loop()
      }
    }

    scala.util
      .Try {
        val res = _loop()
        val r   = txn.commit()
        if (!r) throw new RuntimeException("transaction not flushed, nothing committed")
        else res
      }
      .recover {
        case x: Throwable =>
          txn.abort()
          throw x
      }
      .toEither
  }

  override def transact[A](f: Proxy => A): Either[Throwable, A] = {
    val txn = transaction.getOrElse(environment.beginTransaction())

    runInTransaction(txn) {
      val proxy = new Proxy {
        override def get(key: Array[Byte]): Option[Array[Byte]] = {
          val store =
            environment.openStore(routeStoreName(key), StoreConfig.WITHOUT_DUPLICATES, txn)
          log.debug(s"store: get -> ${routeStoreName(key)}")
          Option(store.get(txn, new ArrayByteIterable(routeStoreKey(key)))).map(_.getBytesUnsafe)
        }
        override def put(key: Array[Byte], value: Array[Byte]): Boolean = {
          val store =
            environment.openStore(routeStoreName(key), StoreConfig.WITHOUT_DUPLICATES, txn)
          log.debug(s"store: put -> ${routeStoreName(key)}")
          store.put(txn, new ArrayByteIterable(routeStoreKey(key)), new ArrayByteIterable(value))
        }

        override def delete(key: Array[Byte]): Boolean = {
          val store =
            environment.openStore(routeStoreName(key), StoreConfig.WITHOUT_DUPLICATES, txn)
          store.delete(txn, new ArrayByteIterable(routeStoreKey(key)))
        }
      }
      f(proxy)
    }
  }
}
