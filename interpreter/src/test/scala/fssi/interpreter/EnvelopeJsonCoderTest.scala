package fssi.interpreter
import fssi.base.BytesValue
import fssi.scp.interpreter.Setting
import fssi.scp.types.{Envelope, Message, NodeID}

object EnvelopeJsonCoderTest extends App {

  val fromJsonString =
    "{\n  \"statement\" : {\n    \"from\" : \"AgUyAdJBRmZQF/fcqbQ/Y6k5AbakymwPmgxVofIzO01X\",\n    \"slotIndex\" : {\n      \"value\" : 3\n    },\n    \"timestamp\" : {\n      \"value\" : 1546064503859\n    },\n    \"quorumSet\" : {\n      \"slices\" : {\n        \"threshold\" : 3,\n        \"validators\" : [\n          \"AqsDUJpYn0zqefI++DjtarDzxFlcjZE7midBtCY4445v\",\n          \"A5DmLPO4uJXWONCCXCIh8IB7Ye5cckL59a9cYc7IrDGu\",\n          \"As3lk1RRKpUZCO9s3YC6wWq51nBi4wj8+mFqLsDOhnjn\",\n          \"AgUyAdJBRmZQF/fcqbQ/Y6k5AbakymwPmgxVofIzO01X\"\n        ],\n        \"inners\" : [\n          {\n            \"threshold\" : 1,\n            \"validators\" : [\n            ]\n          },\n          {\n            \"threshold\" : 1,\n            \"validators\" : [\n            ]\n          }\n        ]\n      }\n    },\n    \"message\" : {\n      \"type\" : \"nominate\",\n      \"body\" : \"{\\\"voted\\\":[{\\\"block\\\":{\\\"height\\\":3,\\\"chainId\\\":\\\"testNet\\\",\\\"preWorldState\\\":\\\"MjhmOTE0NzA1ZTI0OTQwMWU3YmViMTg5M2JjMjE4OTY3MmExZmNiMDNlMWI3MjhhNGMwYzllMjU3MTFiYjI5Zg==\\\",\\\"curWorldState\\\":\\\"\\\",\\\"transactions\\\":[{\\\"type\\\":\\\"Run\\\",\\\"transaction\\\":{\\\"id\\\":\\\"NDRhNjdmOTI4YTc5NDk3YTljMTNiOWQzMWE3NjU5Mzc=\\\",\\\"caller\\\":\\\"1JUPUxGdHQ3x5XeWeMmpVqFK8x1fd8K1uV\\\",\\\"publicKeyForVerifying\\\":\\\"jmgsTT9CPXArPovmvr6wHW5gLiUKMqvLzczBPEx3PMfP\\\",\\\"owner\\\":\\\"1JUPUxGdHQ3x5XeWeMmpVqFK8x1fd8K1uV\\\",\\\"contractName\\\":\\\"dGVzdE5hbWU=\\\",\\\"contractVersion\\\":\\\"1.0.1\\\",\\\"methodAlias\\\":\\\"kvStore\\\",\\\"contractParameter\\\":[\\\"number\\\",\\\"0003333\\\"],\\\"signature\\\":\\\"MEYCIQCNHvke+hb1Ws4buTama9i009badvhn+f2UtGDZ6ev0lwIhAKDYLxuoZlBex0bPf9juc1suiDgAxW0WJlS6PN80gqaG\\\",\\\"timestamp\\\":1544512995679}},{\\\"type\\\":\\\"Run\\\",\\\"transaction\\\":{\\\"id\\\":\\\"MDMzMjcwMDk3N2Q5NDk4NGFlZTUzMjBjMDM2ZGYyZGE=\\\",\\\"caller\\\":\\\"1JUPUxGdHQ3x5XeWeMmpVqFK8x1fd8K1uV\\\",\\\"publicKeyForVerifying\\\":\\\"jmgsTT9CPXArPovmvr6wHW5gLiUKMqvLzczBPEx3PMfP\\\",\\\"owner\\\":\\\"1JUPUxGdHQ3x5XeWeMmpVqFK8x1fd8K1uV\\\",\\\"contractName\\\":\\\"dGVzdE5hbWU=\\\",\\\"contractVersion\\\":\\\"1.0.1\\\",\\\"methodAlias\\\":\\\"kvStore\\\",\\\"contractParameter\\\":[\\\"number\\\",\\\"0004444\\\"],\\\"signature\\\":\\\"MEUCIBV1ZV5JSiULKJLs5E2Enr78Ft79EjSARw3kDM2BLPyyAiEAqizHnHeobk7LE02rCBlkBNmF1OJSnk+qBWLSyCvJbiw=\\\",\\\"timestamp\\\":1544513012057}}],\\\"receipts\\\":[],\\\"timestamp\\\":{\\\"value\\\":1546064501750},\\\"hash\\\":\\\"bsclVOyIt165ud9qXkpZQLNuXOIcePEEIi8PDwYQ+DQ=\\\"}}],\\\"accepted\\\":[]}\"\n    }\n  },\n  \"signature\" : \"MEUCIQCLqWsgiJw3kVbUI86HD83J+to/tC25Qz0jRtJN3kdX0gIgfj76p7YZtEiSLShN/96MoMqNgP/hqk+Bth7Iv56Vf0I=\"\n}"

  import io.circe._
  import io.circe.syntax._
  import io.circe.generic.auto._
  import fssi.scp.interpreter.json.implicits._
  import fssi.types.json.implicits._
  import fssi.interpreter.scp.BlockValue.implicits._
  import fssi.utils._
  crypto.registerBC()

  val encPrivKey =
    BytesValue.unsafeDecodeBcBase58("65fNySGct3T6ARfK2ioU888wKLXVLLuhj6odcfNY1gw7Ctb81Rv5Qsy").bytes
  val iv = BytesValue.unsafeDecodeBcBase58("KJ8Td9eF7qo").bytes
  val ensureKey = crypto.ensure24Bytes(
    BytesValue.unsafeDecodeBcBase58("9AmzL2jkoZBwLMe8WdYpbG9BpCAa4gRpHtHg6vNrdXJM").bytes)
  val privateKeyBytes =
    crypto.des3cbcDecrypt(encPrivKey, ensureKey, iv)
  val privateKey = crypto.rebuildECPrivateKey(privateKeyBytes, crypto.SECP256K1)

  val toJsonString =
    "{\n  \"statement\" : {\n    \"from\" : \"AgUyAdJBRmZQF/fcqbQ/Y6k5AbakymwPmgxVofIzO01X\",\n    \"slotIndex\" : {\n      \"value\" : 3\n    },\n    \"timestamp\" : {\n      \"value\" : 1546064503859\n    },\n    \"quorumSet\" : {\n      \"slices\" : {\n        \"threshold\" : 3,\n        \"validators\" : [\n          \"AqsDUJpYn0zqefI++DjtarDzxFlcjZE7midBtCY4445v\",\n          \"A5DmLPO4uJXWONCCXCIh8IB7Ye5cckL59a9cYc7IrDGu\",\n          \"As3lk1RRKpUZCO9s3YC6wWq51nBi4wj8+mFqLsDOhnjn\",\n          \"AgUyAdJBRmZQF/fcqbQ/Y6k5AbakymwPmgxVofIzO01X\"\n        ],\n        \"inners\" : [\n          {\n            \"threshold\" : 1,\n            \"validators\" : [\n            ]\n          },\n          {\n            \"threshold\" : 1,\n            \"validators\" : [\n            ]\n          }\n        ]\n      }\n    },\n    \"message\" : {\n      \"type\" : \"nominate\",\n      \"body\" : \"{\\\"voted\\\":[{\\\"block\\\":{\\\"height\\\":3,\\\"chainId\\\":\\\"testNet\\\",\\\"preWorldState\\\":\\\"MjhmOTE0NzA1ZTI0OTQwMWU3YmViMTg5M2JjMjE4OTY3MmExZmNiMDNlMWI3MjhhNGMwYzllMjU3MTFiYjI5Zg==\\\",\\\"curWorldState\\\":\\\"\\\",\\\"transactions\\\":[{\\\"type\\\":\\\"Run\\\",\\\"transaction\\\":{\\\"id\\\":\\\"MDMzMjcwMDk3N2Q5NDk4NGFlZTUzMjBjMDM2ZGYyZGE=\\\",\\\"caller\\\":\\\"1JUPUxGdHQ3x5XeWeMmpVqFK8x1fd8K1uV\\\",\\\"publicKeyForVerifying\\\":\\\"jmgsTT9CPXArPovmvr6wHW5gLiUKMqvLzczBPEx3PMfP\\\",\\\"owner\\\":\\\"1JUPUxGdHQ3x5XeWeMmpVqFK8x1fd8K1uV\\\",\\\"contractName\\\":\\\"dGVzdE5hbWU=\\\",\\\"contractVersion\\\":\\\"1.0.1\\\",\\\"methodAlias\\\":\\\"kvStore\\\",\\\"contractParameter\\\":[\\\"number\\\",\\\"0004444\\\"],\\\"signature\\\":\\\"MEUCIBV1ZV5JSiULKJLs5E2Enr78Ft79EjSARw3kDM2BLPyyAiEAqizHnHeobk7LE02rCBlkBNmF1OJSnk+qBWLSyCvJbiw=\\\",\\\"timestamp\\\":1544513012057}},{\\\"type\\\":\\\"Run\\\",\\\"transaction\\\":{\\\"id\\\":\\\"NDRhNjdmOTI4YTc5NDk3YTljMTNiOWQzMWE3NjU5Mzc=\\\",\\\"caller\\\":\\\"1JUPUxGdHQ3x5XeWeMmpVqFK8x1fd8K1uV\\\",\\\"publicKeyForVerifying\\\":\\\"jmgsTT9CPXArPovmvr6wHW5gLiUKMqvLzczBPEx3PMfP\\\",\\\"owner\\\":\\\"1JUPUxGdHQ3x5XeWeMmpVqFK8x1fd8K1uV\\\",\\\"contractName\\\":\\\"dGVzdE5hbWU=\\\",\\\"contractVersion\\\":\\\"1.0.1\\\",\\\"methodAlias\\\":\\\"kvStore\\\",\\\"contractParameter\\\":[\\\"number\\\",\\\"0003333\\\"],\\\"signature\\\":\\\"MEYCIQCNHvke+hb1Ws4buTama9i009badvhn+f2UtGDZ6ev0lwIhAKDYLxuoZlBex0bPf9juc1suiDgAxW0WJlS6PN80gqaG\\\",\\\"timestamp\\\":1544512995679}}],\\\"receipts\\\":[],\\\"timestamp\\\":{\\\"value\\\":1546064501750},\\\"hash\\\":\\\"bsclVOyIt165ud9qXkpZQLNuXOIcePEEIi8PDwYQ+DQ=\\\"}}],\\\"accepted\\\":[]}\"\n    }\n  },\n  \"signature\" : \"MEUCIQCLqWsgiJw3kVbUI86HD83J+to/tC25Qz0jRtJN3kdX0gIgfj76p7YZtEiSLShN/96MoMqNgP/hqk+Bth7Iv56Vf0I=\"\n}"

  import fssi.types.implicits._
  import fssi.scp.types.implicits._

  for {
    fromJson     <- parser.parse(fromJsonString)
    fromEnvelope <- fromJson.as[Envelope[Message]]
    toJson       <- parser.parse(toJsonString)
    toEnvelope   <- toJson.as[Envelope[Message]]
  } yield {
    val fromBytes = fromEnvelope.statement.asBytesValue.bytes
    val toBytes   = toEnvelope.statement.asBytesValue.bytes
    val r         = fromBytes sameElements toBytes
    r

    val fromSignature = crypto.makeSignature(fromBytes, privateKey)

    val verified =
      crypto.verifySignature(
        fromSignature,
        toBytes,
        crypto.rebuildECPublicKey(toEnvelope.statement.from.value, crypto.SECP256K1)
      )
    verified
  }

}
