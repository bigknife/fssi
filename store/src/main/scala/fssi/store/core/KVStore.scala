package fssi.store.core

trait KVStore {
  def get(key: Array[Byte]): Option[Array[Byte]]
  def put(key: Array[Byte], value: Array[Byte]): Either[Throwable, Boolean]
  def delete(key: Array[Byte]): Either[Throwable, Boolean]

  trait Proxy {
    def get(key: Array[Byte]): Option[Array[Byte]]
    def put(key: Array[Byte], value: Array[Byte]): Boolean
    def delete(key: Array[Byte]): Boolean

    /** clean store
      *@param name if the implementation supports multi-store, name is the id of the store instance, or it doesn't make sense
      */
    def clean(name: String): Unit
  }
  def transact[A](f: Proxy => A): Either[Throwable, A]
}
