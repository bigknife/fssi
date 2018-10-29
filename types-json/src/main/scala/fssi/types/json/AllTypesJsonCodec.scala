package fssi
package types
package json

trait AllTypesJsonCodec
    extends BizAccountJsonCodec
    with BizTransactionJsonCodec
    with BizSignatureJsonCodec
    with BizTokenJsonCodec
    with BizContractJsonCodec
    with BaseUniqueNameJsonCodec
    with BaseWorldStateJsonCodec
    with BaseHashJsonCodec
    with BizReceiptJsonCodec
