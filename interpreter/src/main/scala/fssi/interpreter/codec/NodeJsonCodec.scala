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
      "address"     -> a.address.asJson,
      "nodeType" -> Json.fromString(a.nodeType.toString),
      "boundAccount" -> a.boundAccount
        .map(AccountJsonCodec.accountCirceEncoder(_))
        .getOrElse(Json.Null),
      "seeds"     -> Json.fromValues(a.seeds.map(Json.fromString)),
      "runtimeId" -> a.runtimeId.map(_.value).asJson
  )

  implicit val nodeJsonDecoder: Decoder[Node] = (c: HCursor) => {
    for {
      address           <- c.get[Node.Address]("address")
      ip           <- c.get[String]("ip")
      nodeType     <- c.get[String]("nodeType")
      boundAccount <- c.get[Option[Account]]("boundAccount")
      seeds        <- c.get[Vector[String]]("seeds")
      runtimeId    <- c.get[Option[String]]("runtimeId")
    } yield
      Node(
        address,
        Node.Type(nodeType),
        boundAccount,
        seeds,
        runtimeId.map(Node.ID.apply)
      )
  }

  implicit val nodeAddressEncoder: Encoder[Node.Address] = (a: Node.Address) =>
    Json.obj(
      "ip"   -> Json.fromString(a.ip),
      "port" -> Json.fromInt(a.port)
  )
  implicit val nodeAddressDecoder: Decoder[Node.Address] = (c: HCursor) => {
    for {
      ip <- c.get[String]("ip")
      port <- c.get[Int]("port")
    } yield Node.Address(ip, port)
  }
}

object NodeJsonCodec extends NodeJsonCodec
