package fssi
package types

package object json {

  /** the io.circe json codecs.
    * import io.circe._,io.circe.syntax._
    * import fssi.types.json.implicits._
    */
  object implicits
      extends AccountCodec
      with Base64StringJsonCodec
      with HexStringJsonCodec
      with SignatureJsonCodec
      with JsonMessageCodec
      with BlockJsonCodec
      with HashJsonCodec
      with TransactionJsonCodec
      with TokenJsonCodec
      with UniqueNameJsonCodec
      with VersionJsonCodec
      with ContractJsonCodec
}
