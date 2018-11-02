package fssi.store

import fssi.store.types.{StoreKey, StoreKeySet, StoreValue}

trait ChainStore {

  def read(key: StoreKey): Option[StoreValue]
  def readChildrenKeys(key: StoreKey): Option[StoreKeySet]
  def readHash(key: StoreKey): Option[Array[Byte]]
  def readValue(key: StoreKey): Option[Array[Byte]]

  def write(key: StoreKey, value: Array[Byte]): Either[Throwable, Unit]

  def beginTransaction(): ChainStore

  def commit(): ChainStore
  def rollback(): ChainStore
}
