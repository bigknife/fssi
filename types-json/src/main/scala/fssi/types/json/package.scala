package fssi
package types
package object json {
  object implicits extends AccountCodec  with JsonMessageCodec with BlockJsonCodec with HashJsonCodec with TransactionJsonCodec
}
