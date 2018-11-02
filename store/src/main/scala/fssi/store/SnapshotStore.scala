package fssi.store

import fssi.store.types._

trait SnapshotStore {
  def read(key: StoreKey): Option[StoreValue]
  def readChildrenKeys(key: StoreKey): Option[StoreKeySet]
  def readHash(key: StoreKey): Option[Array[Byte]]
  def readValue(key: StoreKey): Option[Array[Byte]]

  def write(key: StoreKey, value: Array[Byte]): Either[Throwable, Unit]

  trait ReadWriteProxy
  def transact[A](f: ReadWriteProxy => A): Either[Throwable, A]

  def commit(): ImmutableStore
  def rollback(): ImmutableStore
}
