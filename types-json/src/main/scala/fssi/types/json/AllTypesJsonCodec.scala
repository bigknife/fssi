package fssi
package types
package json

trait AllTypesJsonCodec
    extends AccountCodec
    with AccountJsonCodec
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
