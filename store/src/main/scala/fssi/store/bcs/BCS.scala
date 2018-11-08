package fssi.store
package bcs

import fssi.base.BytesValue
import fssi.base.BytesValue.implicits._
import fssi.store.bcs.types.{BCSKey, MetaData}
import fssi.store.core.{KVStore, RouteXodusKVStore, XodusKVStore}
import jetbrains.exodus.env.{Environment, Environments}
import mpt._

trait BCS {

  private[bcs] val mpt: MPT

  def getSnapshotMeta(key: MetaKey): Either[Throwable, Option[MetaData]] = {
    // get from meta MPT'
    /*
    (mpt.get(key.snapshotKey) match {
      case Right(None) =>
        mpt.get(key.persistedKey)
      case x => x
    }).right.map(_.map(MetaData))
    */
    mpt.get(key.snapshotKey).map(_.map(MetaData))
  }
  def getPersistedMeta(key: MetaKey): Either[Throwable, Option[MetaData]] = {
    mpt.get(key.persistedKey).map(_.map(MetaData))
  }

  def putMeta(height: BigInt, key: MetaKey, value: MetaData): Either[Throwable, Unit] = {
    // put to meta snapshot
    // and, put to snapshot key store
    // so, we need cross-store transaction support
    //metaSnapshotTrie.
    mpt.transact { proxy =>
      val newKey = key.snapshotKey
      proxy.put(newKey, value.bytes)
      putCachedKeys(proxy, height, newKey)
    }
  }

  // all `put` actions are temporary, when commit, they will be persisted
  def commit(height: BigInt): Either[Throwable, Unit] = {
    mpt.transact {proxy =>
      getCachedKeys(proxy, height).foreach(k =>
        proxy.get(k).foreach {k0 =>
          BCSKey.parseFromSnapshot(k0).foreach{bcsKey =>
            proxy.get(bcsKey.snapshotKey).foreach {data =>
              proxy.put(bcsKey.persistedKey, data)
            }
          }
        }
      )
    }
  }



  private def putCachedKeys(proxy:MPT#Proxy, height: BigInt, newKey: Array[Byte]): Unit = {
    val cachedKeysKey = snapshotKeysCacheKey(height)
    val data = proxy.get(cachedKeysKey).getOrElse(Array.emptyByteArray)
    val s = newKey.asBytesValue.bcBase58
    proxy.put(cachedKeysKey, data ++ s"$s".getBytes("utf-8") ++ Array('\n'.toByte))
  }

  private def getCachedKeys(proxy:MPT#Proxy, height: BigInt): Vector[Array[Byte]] = {
    val cachedKeysKey = snapshotKeysCacheKey(height)
    val data = proxy.get(cachedKeysKey).getOrElse(Array.emptyByteArray)

    def _loop(data: Array[Byte], acc: Vector[Array[Byte]]): Vector[Array[Byte]] = {
      if (data.isEmpty) acc
      else {
        val p = data.indexOf('\n')
        val base58 = new String(data.take(p))
        val next = acc :+ BytesValue.decodeBcBase58(base58).get.bytes
        _loop(data.drop(p + 1), next)
      }
    }

    _loop(data, Vector.empty)
  }

  private def snapshotKeysCacheKey(height: BigInt): Array[Byte] = {
    s"snapshot_keys_cache:$height://allKeys".getBytes("utf-8")
  }
  /*
  def getTransaction(key: TransactionKey, height: BigInt): Option[TransactionData] = {
    // get transaction data from current transaction snapshot
    // if not found in the snapshot, try to get from persisted
  }

  def getReceipt(key: ReceiptKey, height: BigInt): Option[ReceiptData]
  def getState(key: StateKey): Option[StateData]


  def putTransaction(key: TransactionKey, value: TransactionData): Either[Throwable, Unit]
  def putReceipt(key: ReceiptKey, value: ReceiptData): Either[Throwable, Unit]
  def putState(key: StateKey, value: StateData): Either[Throwable, Unit]


  // all `put` actions are temporary, when commit, they will be persisted
  def commit(): Either[Throwable, Unit]
  // all `put` actions are temporary, when rollback, they will be abandoned
  def rollback(): Either[Throwable, Unit]
 */
}

object BCS {
  def apply(root: String): BCS = new BCS {
    val environment: Environment = Environments.newInstance(root)
    Runtime.getRuntime.addShutdownHook(new Thread(() => environment.close()))

    private[bcs] implicit val store: KVStore = {
      new RouteXodusKVStore(environment, None) {
        override def routeStoreName(key: Array[Byte]): String = {
          new String(key.take(key.indexOf('\n')), "utf-8")
        }
        override def routeStoreKey(key: Array[Byte]): Array[Byte] = {
          key.drop(key.indexOf('\n'))
        }
      }
    }

    override private[bcs] val mpt = {
      MPT { keys =>
        // separator is "//"
        val s = keys.take(keys.indexOfSlice(Array('/', '/')))
        new String(s, "utf-8")
      }
    }
  }
}
