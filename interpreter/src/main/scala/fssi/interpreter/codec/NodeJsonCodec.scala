package fssi.interpreter.codec

import fssi.ast.domain.Node
import fssi.ast.domain.types.Account
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.auto._
import io.circe.syntax._

trait NodeJsonCodec extends AccountJsonCodec {
  implicit val nodeJsonEncoder: Encoder[Node] = (a: Node) =>
    Json.obj(
      "id"       -> Json.fromString(a.id.value),
      "port"     -> Json.fromInt(a.port),
      "ip"       -> Json.fromString(a.ip),
      "nodeType" -> Json.fromString(a.nodeType.toString),
      "boundAccount" -> a.boundAccount
        .map(AccountJsonCodec.accountCirceEncoder(_))
        .getOrElse(Json.Null),
      "seeds" -> Json.fromValues(a.seeds.map(Json.fromString)),
      "runtimeId" -> a.runtimeId.map(_.value).asJson
  )

  implicit val nodeJsonDecoder: Decoder[Node] = (c: HCursor) => {
    for {
      id <- c.get[String]("id")
      port <- c.get[Int]("port")
      ip <- c.get[String]("ip")
      nodeType <- c.get[String]("nodeType")
      boundAccount <- c.get[Option[Account]]("boundAccount")
      seeds <- c.get[Vector[String]]("seeds")
      runtimeId <- c.get[Option[String]]("runtimeId")
    } yield
      Node(
        port,
        ip,
        Node.Type(nodeType),
        boundAccount,
        seeds,
        runtimeId.map(Node.ID.apply)
      )
  }
}

object NodeJsonCodec extends NodeJsonCodec
