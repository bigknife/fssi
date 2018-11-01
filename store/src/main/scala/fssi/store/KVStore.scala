package fssi.store

import types._

trait KVStore {
  def get(key: Array[Byte]): Option[Array[Byte]]
  def put(key: Array[Byte], value: Array[Byte]): KVStore
  
}
