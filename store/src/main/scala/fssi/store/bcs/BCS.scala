package fssi.store
package bcs

import fssi.base.BytesValue
import fssi.base.BytesValue.implicits._
import fssi.store.bcs.types._
import fssi.store.bcs.types.BCSKey._
import fssi.store.core.{KVStore, RouteXodusKVStore, XodusKVStore}
import jetbrains.exodus.env.{Environment, Environments}
import mpt._

trait BCS {

  private[bcs] val mpt: MPT

  def close(): Unit

  trait Proxy {
    def getBalance(accountId: String): BigInt
    def putBalance(accountId: String, amount: BigInt): Unit

    def getContractData(accountId: String,
                        contractName: String,
                        appKey: String): Option[Array[Byte]]
    def putContractData(accountId: String,
                        contractName: String,
                        appKey: String,
                        appData: Array[Byte]): Unit
  }

  def snapshotTransact[A](f: Proxy => A): Either[Throwable, A] = {
    mpt.transact { mptProxy =>
      val proxy = new Proxy {
        override def getBalance(accountId: String): BigInt = {
          val balanceKey = StateKey.balance(accountId)
          mptProxy
            .get(balanceKey.snapshotKey)
            .orElse {
              mptProxy.get(balanceKey.persistedKey)
            }
            .map(BigInt(_))
            .getOrElse(0)
        }

        override def putBalance(accountId: String, amount: BigInt): Unit = {
          val balanceKey = StateKey.balance(accountId)
          // this is a snapshot
          mptProxy.put(balanceKey.snapshotKey, amount.toByteArray)
        }

        override def getContractData(accountId: String,
                                     contractName: String,
                                     appKey: String): Option[Array[Byte]] = {
          val contractDataKey = StateKey.contractDb(accountId, contractName, appKey)
          mptProxy
            .get(contractDataKey.snapshotKey)
            .orElse {
              mptProxy.get(contractDataKey.persistedKey)
            }
        }

        override def putContractData(accountId: String,
                                     contractName: String,
                                     appKey: String,
                                     appData: Array[Byte]): Unit = {
          val contractDataKey = StateKey.contractDb(accountId, contractName, appKey)
          mptProxy.put(contractDataKey.snapshotKey, appData)
        }
      }

      f(proxy)
    }
  }

  def getSnapshotMeta(key: MetaKey): Either[Throwable, Option[MetaData]] = {
    mpt.get(key.snapshotKey).map(_.map(MetaData))
  }

  def getSnapshotBlock(key: BlockKey): Either[Throwable, Option[BlockData]] = {
    mpt.get(key.snapshotKey).map(_.map(BlockData))
  }

  def getSnapshotTransaction(key: TransactionKey): Either[Throwable, Option[TransactionData]] = {
    mpt.get(key.snapshotKey).map(_.map(TransactionData))
  }

  def getSnapshotReceipt(key: ReceiptKey): Either[Throwable, Option[ReceiptData]] = {
    mpt.get(key.snapshotKey).map(_.map(ReceiptData))
  }

  def getSnapshotState(key: StateKey): Either[Throwable, Option[StateData]] = {
    mpt.get(key.snapshotKey).map(_.map(StateData))
  }

  def getPersistedMeta(key: MetaKey): Either[Throwable, Option[MetaData]] = {
    mpt.get(key.persistedKey).map(_.map(MetaData))
  }

  def getPersistedBlock(key: BlockKey): Either[Throwable, Option[BlockData]] = {
    mpt.get(key.persistedKey).map(_.map(BlockData))
  }

  def getPersistedTransaction(key: TransactionKey): Either[Throwable, Option[TransactionData]] = {
    mpt.get(key.persistedKey).map(_.map(TransactionData))
  }

  def getPersistedReceipt(key: ReceiptKey): Either[Throwable, Option[ReceiptData]] = {
    mpt.get(key.persistedKey).map(_.map(ReceiptData))
  }

  def getPersistedState(key: StateKey): Either[Throwable, Option[StateData]] = {
    mpt.get(key.persistedKey).map(_.map(StateData))
  }

  def getPersistedBalance(accountId: String): Either[Throwable, BigInt] = {
    getPersistedState(StateKey.balance(accountId)).right.map { x =>
      x.map { y =>
          BigInt(y.bytes)
        }
        .getOrElse(0)
    }
  }

  def getSnapshotBalance(accountId: String): Either[Throwable, BigInt] = {
    getSnapshotState(StateKey.balance(accountId)).right.map { x =>
      x.map { y =>
          BigInt(y.bytes)
        }
        .getOrElse(0)
    }
  }

  def getMetaGreedily(key: MetaKey): Either[Throwable, Option[MetaData]] = {
    getSnapshotMeta(key).right.flatMap {
      case None =>
        getPersistedMeta(key)
      case x => Right(x)
    }
  }

  def getBlockGreedily(key: BlockKey): Either[Throwable, Option[BlockData]] = {
    getSnapshotBlock(key).right.flatMap {
      case None =>
        getPersistedBlock(key)
      case x => Right(x)
    }
  }

  def getTransactionGreedily(key: TransactionKey): Either[Throwable, Option[TransactionData]] = {
    getSnapshotTransaction(key).right.flatMap {
      case None =>
        getPersistedTransaction(key)
      case x => Right(x)
    }
  }

  def getReceiptGreedily(key: ReceiptKey): Either[Throwable, Option[ReceiptData]] = {
    getSnapshotReceipt(key).right.flatMap {
      case None =>
        getPersistedReceipt(key)
      case x => Right(x)
    }
  }

  def getStateGreedily(key: StateKey): Either[Throwable, Option[StateData]] = {
    getSnapshotState(key).right.flatMap {
      case None =>
        getPersistedState(key)
      case x => Right(x)
    }
  }

  def putMeta(height: BigInt, key: MetaKey, value: BlockData): Either[Throwable, Unit] = {
    // we need cross-store transaction support
    mpt.transact { proxy =>
      val newKey = key.snapshotKey
      proxy.put(newKey, value.bytes)
      putCachedKeys(proxy, height, newKey)
    }
  }

  def putBlock(key: BlockKey, value: BlockData): Either[Throwable, Unit] = {
    // we need cross-store transaction support
    mpt.transact { proxy =>
      val newKey = key.snapshotKey
      proxy.put(newKey, value.bytes)
      putCachedKeys(proxy, key.height, newKey)
    }
  }

  def putTransaction(key: TransactionKey, value: TransactionData): Either[Throwable, Unit] = {
    // we need cross-store transaction support
    mpt.transact { proxy =>
      val newKey = key.snapshotKey
      proxy.put(newKey, value.bytes)
      putCachedKeys(proxy, key.height, newKey)
    }
  }

  def putReceipt(key: ReceiptKey, value: ReceiptData): Either[Throwable, Unit] = {
    // we need cross-store transaction support
    mpt.transact { proxy =>
      val newKey = key.snapshotKey
      proxy.put(newKey, value.bytes)
      putCachedKeys(proxy, key.height, newKey)
    }
  }

  def putState(height: BigInt, key: StateKey, value: StateData): Either[Throwable, Unit] = {
    // we need cross-store transaction support
    mpt.transact { proxy =>
      val newKey = key.snapshotKey
      proxy.put(newKey, value.bytes)
      putCachedKeys(proxy, height, newKey)
    }
  }

  def putBalance(height: BigInt, accountId: String, balance: BigInt): Either[Throwable, Unit] = {
    putState(height, StateKey.balance(accountId), StateData(balance.toByteArray))
  }

  // all `put` actions are temporary, when commit, they will be persisted
  def commit(height: BigInt): Either[Throwable, Unit] = {
    mpt.transact { proxy =>
      // after copy we should clean
      getCachedKeys(proxy, height).foreach(k =>
        proxy.get(k).foreach { data =>
          BCSKey.parseFromSnapshot(k).foreach { bcsKey =>
            proxy.put(bcsKey.persistedKey, data)
          }
      })

      // clean
      getCachedKeys(proxy, height).foreach(k =>
        proxy.get(k).foreach { _ =>
          BCSKey.parseFromSnapshot(k).foreach { bcsKey =>
            proxy.clean(bcsKey.snapshotKey)
          }
      })
    }
  }

  // all `put` actions are temporary, when rollback, they will be abandoned
  def rollback(height: BigInt): Either[Throwable, Unit] = {
    mpt.transact { proxy =>
      getCachedKeys(proxy, height).foreach(k =>
        proxy.get(k).foreach { _ =>
          BCSKey.parseFromSnapshot(k).foreach { bcsKey =>
            proxy.clean(bcsKey.snapshotKey)
          }
      })
    }
  }

  private def putCachedKeys(proxy: MPT#Proxy, height: BigInt, newKey: Array[Byte]): Unit = {
    val cachedKeysKey = snapshotKeysCacheKey(height)

    val keySet: Set[String] = proxy
      .get(cachedKeysKey)
      .map(new String(_))
      .map(_.split("\n"))
      .filter(_.nonEmpty)
      .toSet
      .flatten

    val s = newKey.asBytesValue.bcBase58

    val newData = (keySet + s).mkString("\n").getBytes("utf-8")

    proxy.put(cachedKeysKey, newData)
  }

  private def getCachedKeys(proxy: MPT#Proxy, height: BigInt): Vector[Array[Byte]] = {
    val cachedKeysKey = snapshotKeysCacheKey(height)
    val data          = proxy.get(cachedKeysKey).getOrElse(Array.emptyByteArray)

    def _loop(data: Array[Byte], acc: Vector[Array[Byte]]): Vector[Array[Byte]] = {
      if (data.isEmpty) acc
      else {
        val p = data.indexOf('\n')
        if (p == -1) {
          val base58 = new String(data)
          acc :+ BytesValue.decodeBcBase58(base58).get.bytes
        } else {
          val base58 = new String(data.take(p))
          val next   = acc :+ BytesValue.decodeBcBase58(base58).get.bytes
          _loop(data.drop(p + 1), next)
        }
      }
    }

    _loop(data, Vector.empty)
  }

  private def snapshotKeysCacheKey(height: BigInt): Array[Byte] = {
    s"snapshot_keys_cache:$height://allKeys".getBytes("utf-8")
  }
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
      MPT(
        keys => {
          // separator is "//"
          val s = keys.take(keys.indexOfSlice(Array('/', '/')))
          new String(s, "utf-8")
        },
        (storeName, key) => {
          storeName.getBytes("utf-8") ++ Array('\n'.toByte) ++ key
        }
      )
    }

    override def close(): Unit = environment.close()
  }
}
