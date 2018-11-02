package fssi.store

import fssi.store.types._
import java.io._
import jetbrains.exodus.env._

trait ImmutableStore {
  def read(key: StoreKey): Option[StoreValue]
  def readChildrenKeys(key: StoreKey): Option[StoreKeySet]
  def readHash(key: StoreKey): Option[Array[Byte]]
  def readValue(key: StoreKey): Option[Array[Byte]]

  def beginTransaction(): SnapshotStore
}

object ImmutableStore {
  def load(root: File): ImmutableStore = new ImmutableStore {outter =>
    val FSSI_STORE          = "fssi_store"
    val FSSI_STORE_SNAPSHOT = "fssi_store_snapshot"

    val environment: Environment     = Environments.newInstance(root)
    val immutableStore: XodusKVStore = new XodusKVStore(FSSI_STORE, environment, None)

    def read(key: StoreKey): Option[StoreValue] =
      for {
        bytes    <- readValue(key)
        hash     <- readHash(key)
        children <- readChildrenKeys(key).orElse(Some(StoreKeySet.empty))
      } yield StoreValue(bytes, children, hash)

    def readChildrenKeys(key: StoreKey): Option[StoreKeySet] = {
      val childrenKeysKey = key.withTag("children").bytesValue
      immutableStore.get(childrenKeysKey).map(StoreKeySet.fromBytes)
    }

    def readHash(key: StoreKey): Option[Array[Byte]] = {
      val hashKey = key.withTag("hash").bytesValue
      immutableStore.get(hashKey)
    }

    def readValue(key: StoreKey): Option[Array[Byte]] = {
      val bytesKey = key.withTag("bytes").bytesValue
      immutableStore.get(bytesKey)
    }

    def beginTransaction(): SnapshotStore = new SnapshotStore {
      def snapshotTransactionStore: XodusKVStore = {
        val transaction = environment.beginTransaction()
        new XodusKVStore(FSSI_STORE_SNAPSHOT, environment, Some(transaction))
      }

      override def read(key: StoreKey): Option[StoreValue] = ???

      override def readChildrenKeys(key: StoreKey): Option[StoreKeySet] = ???

      override def readHash(key: StoreKey): Option[Array[Byte]] = ???

      override def readValue(key: StoreKey): Option[Array[Byte]] = {


      }

      override def write(key: StoreKey, value: Array[Byte]): Either[Throwable, Unit] = ???

      override def transact[A](f: ReadWriteProxy => A): Either[Throwable, A] = ???

      override def commit(): ImmutableStore = ???

      override def rollback(): ImmutableStore = ???

      private def _transact[A](store: XodusKVStore)(block: => A): A = {

      }
    }

    private def _get(store: KVStore, key: StoreKey): Option[StoreValue] = {
      val bytesKey        = key.withTag("bytes").bytesValue
      val childrenKeysKey = key.withTag("children").bytesValue
      val hashKey         = key.withTag("hash").bytesValue
      for {
        bytes        <- store.get(bytesKey)
        childrenKeys <- store.get(childrenKeysKey).orElse(Option(Array.emptyByteArray))
        hash         <- store.get(hashKey)
      } yield StoreValue(bytes, StoreKeySet.fromBytes(childrenKeys), hash)
    }

    private def _put(store: KVStore, key: StoreKey, value: Array[Byte]): Either[Throwable, Unit] = {
      scala.util.Try {
        store.put(key.withTag("bytes").bytesValue, value)
        // update hash recursively to upside, satisfy merkle tree
        updateHash(store, key)
        // update parent's childrenKeys
        key.previousLevel.foreach { parent =>
          val parentChildrenKey = parent.withTag("children").bytesValue
          val children = store
            .get(parentChildrenKey)
            .map(StoreKeySet.fromBytes)
            .getOrElse(StoreKeySet.empty)
          val newKey = StoreKeySet.toBytes(children + key)
          store.put(parentChildrenKey, newKey)
        }
        ()
      }.toEither
    }

    private def updateHash(store: KVStore, key: StoreKey): Unit = {
      // ensure value has been put
      val value = store.get(key.withTag("bytes").bytesValue).get
      val hash: Array[Byte] = {
        val source = store
          .get(key.withTag("children").bytesValue)
          .map(StoreKeySet.fromBytes)
          .map { keySet =>
            keySet.foldLeft(Array.emptyByteArray) { (acc, n) =>
              acc ++ store.get(n.withTag("hash").bytesValue).get
            }
          }
          .map(_ ++ value)
          .getOrElse(value)
        sha256(source)
      }
      store.put(key.withTag("hash").bytesValue, hash)
      val parent = key.previousLevel
      if (parent.nonEmpty) updateHash(store, parent.get)
      else ()
    }

    private def sha256(source: Array[Byte]): Array[Byte] = {
      java.security.MessageDigest.getInstance("SHA-256").digest(source)
    }
  }
}
