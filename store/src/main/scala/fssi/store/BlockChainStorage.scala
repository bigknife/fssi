package fssi.store

import types._
import java.io._

import jetbrains.exodus.ArrayByteIterable
import jetbrains.exodus.env._
import org.slf4j.LoggerFactory

trait BlockChainStorage {
  type This = this.type
  private[store] val readOnlyStore: KVStore
  private[store] val transactor: Transactor

  def init(): Unit
  def close(): Unit

  def get(key: StoreKey): Option[StoreValue] = _get(readOnlyStore, key)

  def put(key: StoreKey, value: Array[Byte]): Either[Throwable, Unit] = {
    transactor.transact { store =>
      _put(store, key, value)
      ()
    }
  }

  def puts(kvs: Map[StoreKey, Array[Byte]]): Either[Throwable, Unit] = {
    transactor.transact { store =>
      kvs.foldLeft(Right(()): Either[Throwable, Unit]) {
        case (acc, (k, v)) =>
          acc match {
            case x @ Left(_) => x
            case _           => _put(store, k, v)
          }
      }
      ()
    }
  }

  trait ReadWriter {
    def get(key: StoreKey): Option[StoreValue]
    def put(key: StoreKey, value: Array[Byte]): Either[Throwable, Unit]
  }

  def transact[A](f: ReadWriter => A): Either[Throwable, A] = transactor.transact { store =>
    val rw = new ReadWriter {
      override def get(key: StoreKey): Option[StoreValue] = _get(store, key)
      override def put(key: StoreKey, value: Array[Byte]): Either[Throwable, Unit] =
        _put(store, key, value)
    }
    f(rw)
  }

  /** satisfy merkle tree*/
  private def _put(store: KVStore, key: StoreKey, value: Array[Byte]): Either[Throwable, Unit] = {
    val bytesKey        = key.withTag("bytes").bytesValue
    val childrenKeysKey = key.withTag("children").bytesValue
    val hashKey         = key.withTag("hash").bytesValue

    //todo: compute childrenKeys and hash
    val (childrenKeys, hash) = {
      (Array.emptyByteArray, Array.emptyByteArray)
    }

    scala.util.Try {
      store
        .put(bytesKey, value)
        .put(childrenKeysKey, childrenKeys)
        .put(hashKey, hash)

      // todo: satisfy merkle tree
      ()
    }.toEither
  }

  private def _get(store: KVStore, key: StoreKey): Option[StoreValue] = {
    val bytesKey        = key.withTag("bytes").bytesValue
    val childrenKeysKey = key.withTag("children").bytesValue
    val hashKey         = key.withTag("hash").bytesValue
    for {
      bytes        <- store.get(bytesKey)
      childrenKeys <- store.get(childrenKeysKey)
      hash         <- store.get(hashKey)
    } yield StoreValue(bytes, StoreKeySet.fromBytes(childrenKeys), hash)
  }

}

object BlockChainStorage {
  private val log = LoggerFactory.getLogger(getClass)

  def xodus(root: File): BlockChainStorage = new BlockChainStorage {
    private val env                                       = Environments.newInstance(root)
    override private[store] val readOnlyStore: XodusStore = new XodusStore(None, env)
    override private[store] val transactor = new Transactor {

      /** `f` is a tranction, if `f` succeed, the transaction should be committed, or rollbacked.
        */
      override def transact[A](f: KVStore => A): Either[Throwable, A] = {
        val transaction = env.beginTransaction()
        val transStore  = new XodusStore(Some(transaction), env)
        try {
          def _loop(): A = {
            val r = f(transStore)
            if (transaction.flush()) r
            else {
              transaction.revert()
              _loop()
            }
          }
          val a = _loop()
          transaction.commit()
          Right(a)
        } catch {
          case x: Throwable =>
            transaction.abort()
            Left(x)
        }

      }
    }

    override def init(): Unit = ()
    override def close(): Unit = {
      env.close()
    }
  }

  class XodusStore(transaction: Option[Transaction], env: Environment) extends KVStore {

    override def get(key: Array[Byte]): Option[Array[Byte]] = {
      val txn = transaction.getOrElse(env.beginReadonlyTransaction())
      def block: Option[Array[Byte]] = {
        val store = env.openStore("MyStore", StoreConfig.WITHOUT_DUPLICATES, txn)
        Option(store.get(txn, new ArrayByteIterable(key))).map(_.getBytesUnsafe)
      }
      val r = block
      if (transaction.isEmpty) txn.abort()
      r
    }

    override def put(key: Array[Byte], value: Array[Byte]): KVStore = {
      val txn = transaction.getOrElse(env.beginTransaction())
      def block(): Unit = {
        val store = env.openStore("MyStore", StoreConfig.WITHOUT_DUPLICATES, txn)
        val r     = store.put(txn, new ArrayByteIterable(key), new ArrayByteIterable(value))

        ()
      }
      if (transaction.isEmpty) {
        def _loop(): Unit = {
          val r: Unit = block()
          if (txn.flush()) r
          else {
            txn.revert()
            _loop()
          }
        }

        try {
          _loop()
          txn.commit()
          this
        } catch {
          case x: Throwable =>
            log.error("store failed", x)
            txn.abort()
            this
        }

      } else {
        block()
        this
      }
    }
  }
}
