package fssi.types

/** Common, Type-Unknown, or User-Customized data
  */
case class Data(value: Array[Byte])

object Data {
  def empty: Data = Data(Array.emptyByteArray)
}
