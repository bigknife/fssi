package fssi
package types
package json

trait AllTypesJsonCodec
    extends AccountCodec //deprecated
    with AccountJsonCodec
    with Base64StringJsonCodec
    with HexStringJsonCodec
    with SignatureJsonCodec
    with JsonMessageCodec
    with BlockJsonCodec
    with HashJsonCodec
    with TransactionJsonCodec //deprecated
    with BizTransactionJsonCodec
    with BizSignatureJsonCodec
    with BizTokenJsonCodec
    with BizContractJsonCodec
    with BaseUniqueNameJsonCodec
    with BaseWorldStateJsonCodec
    with TokenJsonCodec
    with UniqueNameJsonCodec
    with VersionJsonCodec
    with ContractJsonCodec
//    with io.circe.generic.AutoDerivation
