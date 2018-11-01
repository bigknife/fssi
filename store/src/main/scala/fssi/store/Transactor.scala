package fssi.store

trait Transactor {
  /** `f` is a tranction, if `f` succeed, the transaction should be committed, or rollbacked.
    */
  def transact[A](f: KVStore => A): Either[Throwable, A]
}
