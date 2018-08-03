package fssi.types

/**
  * DataBlock is an abstraction of any data.
  * the real data is saved somewhere, and it's referenced by an index.
  *
  * We load the data index instantly, but load the real data lazily for
  * imporovin performance.
  *
  * DataBlock is just like an key-value database, So it's should be easy
  * implemented.
  */
sealed trait DataBlock {
  def keys: Set[DataBlock.Key]
  def put(key: DataBlock.Key, value: DataBlock.

    Value): Unit
  def get(key: DataBlock.Key): Option[DataBlock.Value]
}

object DataBlock {
  case class Index(value: HexString)

  case class Key(value: Array[Byte])
  case class Value(value: Array[Byte])

  /**
    * Create an empty value object.
    */
  def emptyValue: Value = Value(Array.emptyByteArray)
}
