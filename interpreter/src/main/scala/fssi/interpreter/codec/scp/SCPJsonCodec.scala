package fssi.interpreter.codec.scp

import bigknife.scalap.ast.types.Message._
import bigknife.scalap.ast.types._
import fssi.interpreter.scp.MomentValue
import io.circe.{Encoder, Json}
import fssi.interpreter.jsonCodec._
import io.circe.syntax._
import io.circe.generic.auto._

trait SCPJsonCodec {
  implicit val valueJsonEncoder: Encoder[Value] = new Encoder[Value] {
    override def apply(a: Value): Json = a match {
      case MomentValue(moment) => moment.asJson
      case _                   => a.asHex().asJson
    }
  }

  implicit val valueSetJsonEncoder: Encoder[ValueSet] = new Encoder[ValueSet] {
    override def apply(a: ValueSet): Json = a.toVector.asJson
  }

  implicit val messageJsonEncoder: Encoder[Message] = new Encoder[Message] {
    override def apply(a: Message): Json = a match {
      case x: Nomination  => x.asJson
      case x: Prepare     => x.asJson
      case x: Commit      => x.asJson
      case x: Externalize => x.asJson
    }
  }

  implicit def statementJsonEncoder[M <: Message]: Encoder[Statement[M]] =
    new Encoder[Statement[M]] {
      override def apply(a: Statement[M]): Json = a match {
        case x: Statement.Nominate    => x.asJson
        case x: Statement.Commit      => x.asJson
        case x: Statement.Externalize => x.asJson
        case x: Statement.Prepare     => x.asJson
      }
    }

  implicit val signatureJsonEncoder: Encoder[Signature] = new Encoder[Signature] {
    override def apply(a: Signature): Json = a.asHex().asJson
  }

  implicit def envelopeJsonEncoder[M <: Message]: Encoder[Envelope[M]] = new Encoder[Envelope[M]] {
    override def apply(a: Envelope[M]): Json = a match {
      case x @ Envelope.NominationEnvelope(_, _) => x.asJson
      case x: Envelope[_] =>
        Json.obj(
          "statement" -> x.statement.asJson,
          "signature" -> x.signature.asJson
        )

    }
  }
}
