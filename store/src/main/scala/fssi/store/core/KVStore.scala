package fssi.store.core

trait KVStore {
  def get(key: Array[Byte]): Option[Array[Byte]]
  def put(key: Array[Byte], value: Array[Byte]): Either[Throwable, Boolean]
  def delete(key: Array[Byte]): Either[Throwable, Boolean]
  def keyIterator: Iterator[Array[Byte]]

  trait Proxy {
    def get(key: Array[Byte]): Option[Array[Byte]]
    def put(key: Array[Byte], value: Array[Byte]): Boolean
    def delete(key: Array[Byte]): Boolean
  }
  def transact[A](f: Proxy => A): Either[Throwable, A]
}
