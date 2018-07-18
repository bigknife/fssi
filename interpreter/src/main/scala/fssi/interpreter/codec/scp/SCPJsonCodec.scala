package fssi.interpreter.codec.scp

import bigknife.scalap.ast.types.Message._
import bigknife.scalap.ast.types._
import fssi.ast.domain.types.{BytesValue, Moment}
import fssi.interpreter.scp.{MomentValue, QuorumSetSync}
import io.circe._
import fssi.interpreter.jsonCodec._
import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.generic.auto._

import scala.collection.immutable

trait SCPJsonCodec {
  implicit val valueJsonEncoder: Encoder[Value] = new Encoder[Value] {
    override def apply(a: Value): Json = a match {
      case MomentValue(moment) => moment.asJson
      case _                   => a.asHex().asJson
    }
  }

  // only support MomentValue
  implicit val valueJsonDecoder: Decoder[Value] = new Decoder[Value] {
    override def apply(c: HCursor): Result[Value] = {
      for {
        moments <- c.value.as[Vector[Moment]]
      } yield MomentValue(moments)
    }
  }

  implicit val valueSetJsonEncoder: Encoder[ValueSet] = new Encoder[ValueSet] {
    override def apply(a: ValueSet): Json = a.toVector.asJson
  }

  implicit val valueSetJsonDecoder: Decoder[ValueSet] = new Decoder[ValueSet] {
    override def apply(c: HCursor): Result[ValueSet] =
      for {
        vec <- c.value.as[Vector[Value]]
      } yield ValueSet(vec: _*)
  }

  implicit val messageJsonEncoder: Encoder[Message] = new Encoder[Message] {
    override def apply(a: Message): Json = a match {
      case x: Nomination  => x.asJson
      case x: Prepare     => x.asJson
      case x: Commit      => x.asJson
      case x: Externalize => x.asJson
    }
  }

  implicit val messageNominationDecoder: Decoder[Message.Nomination] = new Decoder[Nomination] {
    override def apply(c: HCursor): Result[Nomination] =
      for {
        voted    <- c.get[ValueSet]("voted")
        accepted <- c.get[ValueSet]("accepted")
      } yield Message.Nomination(voted, accepted)
  }

  implicit val ballotJsonDecoder: Decoder[Ballot] = new Decoder[Ballot] {
    override def apply(c: HCursor): Result[Ballot] = {
      val counter = c.get[Int]("counter")
      counter.flatMap {
        case x if x == 0 => Right(Ballot.Null)
        case x => for {
          v <- c.get[Value]("value")
        } yield Ballot(x, v)
      }
    }
  }

  implicit val messagePrepareDecoder: Decoder[Message.Prepare] = new Decoder[Prepare] {
    override def apply(c: HCursor): Result[Prepare] =
      for {
        ballot        <- c.get[Ballot]("ballot")
        prepared      <- c.get[Ballot]("prepared")
        preparedPrime <- c.get[Ballot]("preparedPrime")
        hCounter      <- c.get[Int]("hCounter")
        cCounter      <- c.get[Int]("cCounter")
      } yield Message.Prepare(ballot, prepared, preparedPrime, hCounter, cCounter)
  }

  implicit val messageCommitDecoder: Decoder[Message.Commit] = new Decoder[Commit] {
    override def apply(c: HCursor): Result[Commit] =
      for {
        ballot          <- c.get[Ballot]("ballot")
        preparedCounter <- c.get[Int]("preparedCounter")
        hCounter        <- c.get[Int]("hCounter")
        cCounter        <- c.get[Int]("cCounter")
      } yield Message.Commit(ballot, preparedCounter, hCounter, cCounter)
  }

  implicit val messageExternalizeDecoder: Decoder[Message.Externalize] = new Decoder[Externalize] {
    override def apply(c: HCursor): Result[Externalize] =
      for {
        ballot   <- c.get[Ballot]("commit")
        hCounter <- c.get[Int]("hCounter")
      } yield Message.Externalize(ballot, hCounter)
  }

  implicit val nodeIDJsonEncoder: Encoder[NodeID] = new Encoder[NodeID] {
    override def apply(a: NodeID): Json = a.asHex().asJson
  }
  implicit val nodeIDJsonDecoder: Decoder[NodeID] = new Decoder[NodeID] {
    override def apply(c: HCursor): Result[NodeID] = {
      for {
        hex <- c.as[String]
      } yield NodeID(BytesValue.decodeHex(hex).bytes)
    }
  }
  implicit val slotIndexJsonEncoder: Encoder[SlotIndex] = new Encoder[SlotIndex] {
    override def apply(a: SlotIndex): Json = a.asHex().asJson
  }
  implicit val slotIndexJsonDecoder: Decoder[SlotIndex] = new Decoder[SlotIndex] {
    override def apply(c: HCursor): Result[SlotIndex] = {
      for {
        hex <- c.as[String]
      } yield SlotIndex(BigInt(BytesValue.decodeHex(hex).bytes))
    }
  }
  implicit val scpHashJsonEncoder: Encoder[Hash] = new Encoder[Hash] {
    override def apply(a: Hash): Json = a.asHex().asJson
  }
  implicit val scpHashJsonDecoder: Decoder[Hash] = new Decoder[Hash] {
    override def apply(c: HCursor): Result[Hash] =
      for {
        hex <- c.as[String]
      } yield Hash(BytesValue.decodeHex(hex).bytes)
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

  implicit val signatureJsonEncoder: Encoder[Signature] =
    new Encoder[Signature] {
      override def apply(a: Signature): Json = a.asHex().asJson
    }

  implicit val signatureJsonDecoder: Decoder[Signature] = new Decoder[Signature] {
    override def apply(c: HCursor): Result[Signature] = {
      for {
        x <- c.value.as[String]
      } yield Signature(BytesValue.decodeHex(x).bytes)

    }
  }

  implicit def envelopeJsonEncoder[M <: Message]: Encoder[Envelope[M]] =
    new Encoder[Envelope[M]] {
      override def apply(a: Envelope[M]): Json = a match {
        case x: Envelope[_] =>
          x.statement match {
            case _: Statement.Nominate =>
              Json.obj(
                "statement" -> x.statement.asJson,
                "signature" -> x.signature.asJson,
                "type"      -> Json.fromString("nominate")
              )
            case _: Statement.Prepare =>
              Json.obj(
                "statement" -> x.statement.asJson,
                "signature" -> x.signature.asJson,
                "type"      -> Json.fromString("prepare")
              )
            case _: Statement.Commit =>
              Json.obj(
                "statement" -> x.statement.asJson,
                "signature" -> x.signature.asJson,
                "type"      -> Json.fromString("commit")
              )
            case _: Statement.Externalize =>
              Json.obj(
                "statement" -> x.statement.asJson,
                "signature" -> x.signature.asJson,
                "type"      -> Json.fromString("externalize")
              )
          }
      }
    }

  implicit val statementNominateJsonDecoder: Decoder[Statement.Nominate] =
    new Decoder[Statement.Nominate] {
      override def apply(c: HCursor): Result[Statement.Nominate] = {
        for {
          nodeID        <- c.get[NodeID]("nodeID")
          slotIndex     <- c.get[SlotIndex]("slotIndex")
          quorumSetHash <- c.get[Hash]("quorumSetHash")
          message       <- c.get[Message.Nomination]("message")
        } yield Statement.Nominate(nodeID, slotIndex, quorumSetHash, message)
      }
    }

  implicit val statementCommitJsonDecoder: Decoder[Statement.Commit] =
    new Decoder[Statement.Commit] {
      override def apply(c: HCursor): Result[Statement.Commit] = {
        for {
          nodeID        <- c.get[NodeID]("nodeID")
          slotIndex     <- c.get[SlotIndex]("slotIndex")
          quorumSetHash <- c.get[Hash]("quorumSetHash")
          message       <- c.get[Message.Commit]("message")
        } yield Statement.Commit(nodeID, slotIndex, quorumSetHash, message)
      }
    }

  implicit val statementPrepareJsonDecoder: Decoder[Statement.Prepare] =
    new Decoder[Statement.Prepare] {
      override def apply(c: HCursor): Result[Statement.Prepare] = {
        for {
          nodeID        <- c.get[NodeID]("nodeID")
          slotIndex     <- c.get[SlotIndex]("slotIndex")
          quorumSetHash <- c.get[Hash]("quorumSetHash")
          message       <- c.get[Message.Prepare]("message")
        } yield Statement.Prepare(nodeID, slotIndex, quorumSetHash, message)
      }
    }

  implicit val statementExternalizeJsonDecoder: Decoder[Statement.Externalize] =
    new Decoder[Statement.Externalize] {
      override def apply(c: HCursor): Result[Statement.Externalize] = {
        for {
          nodeID        <- c.get[NodeID]("nodeID")
          slotIndex     <- c.get[SlotIndex]("slotIndex")
          quorumSetHash <- c.get[Hash]("quorumSetHash")
          message       <- c.get[Message.Externalize]("message")
        } yield Statement.Externalize(nodeID, slotIndex, quorumSetHash, message)
      }
    }

  implicit val envelopeJsonDecoder: Decoder[Envelope[Message]] =
    new Decoder[Envelope[Message]] {
      override def apply(c: HCursor): Result[Envelope[Message]] = {
        c.get[String]("type").flatMap {
          case "nominate" =>
            for {
              st  <- c.get[Statement.Nominate]("statement")
              sig <- c.get[Signature]("signature")
            } yield Envelope.NominationEnvelope(st, sig)

          case "prepare" =>
            for {
              st  <- c.get[Statement.Prepare]("statement")
              sig <- c.get[Signature]("signature")
            } yield Envelope.BallotEnvelope(st, sig)

          case "commit" =>
            for {
              st  <- c.get[Statement.Commit]("statement")
              sig <- c.get[Signature]("signature")
            } yield Envelope.BallotEnvelope(st, sig)

          case "externalize" =>
            for {
              st  <- c.get[Statement.Externalize]("statement")
              sig <- c.get[Signature]("signature")
            } yield Envelope.BallotEnvelope(st, sig)
        }
      }
    }

  implicit val quorumSetJsonEncoder: Encoder[QuorumSet] = new Encoder[QuorumSet] {
    override def apply(a: QuorumSet): Json = a match {
      case x@ QuorumSet.Simple(threashold, validators) => x.asJson
      case x@ QuorumSet.Nest(threshold, validators, innerSets) => x.asJson
    }
  }

  implicit val quorumSetJsonDecoder: Decoder[QuorumSet] = new Decoder[QuorumSet] {
    override def apply(c: HCursor): Result[QuorumSet] = {
      c.get[Set[QuorumSet.Simple]]("innerSets") match {
        case Left(_) => for {
          threshold <- c.get[Int]("threshold")
          validators <- c.get[Set[NodeID]]("validators")
        } yield QuorumSet.Simple(threshold, validators)

        case Right(innerSets) => for {
          threshold <- c.get[Int]("threshold")
          validators <- c.get[Set[NodeID]]("validators")
        } yield QuorumSet.Nest(threshold, validators, innerSets)
      }
    }
  }

  implicit val quorumSetsJsonEncoder: Encoder[Map[NodeID, QuorumSet]] = new Encoder[Map[NodeID, QuorumSet]] {
    override def apply(a: Map[NodeID, QuorumSet]): Json = Json.fromValues(a.toVector.map {
      case (nodeID, quorumSet) => Json.obj(
        "nodeID" -> nodeID.asJson,
        "qs" -> quorumSet.asJson
      )
    })
  }

  implicit val quorumSetsJsonDecoder: Decoder[Map[NodeID, QuorumSet]] = new Decoder[Map[NodeID, QuorumSet]] {
    override def apply(c: HCursor): Result[Map[NodeID, QuorumSet]] = {

        val s: immutable.Seq[Either[DecodingFailure, (NodeID, QuorumSet)]] = c.value.asArray.get.map { json =>
          val jso = json.asObject.get
          for {
            nodeID <- jso("nodeID").get.as[NodeID]
            qs <- jso("qs").get.as[QuorumSet]
          } yield nodeID -> qs
        }
      Right(s.map(_.right.get).toMap)
    }
  }

  implicit val quorumSetSyncJsonEncoder: Encoder[QuorumSetSync] = new Encoder[QuorumSetSync] {
    override def apply(a: QuorumSetSync): Json = Json.obj(
      "version" -> Json.fromLong(a.version),
      "registeredQuorumSets" -> a.registeredQuorumSets.asJson,
      "hash" -> a.hash.asJson
    )
  }

  implicit val quorumSetSyncJsonDecoder: Decoder[QuorumSetSync] = new Decoder[QuorumSetSync] {
    override def apply(c: HCursor): Result[QuorumSetSync] = {
      for {
        version <- c.get[Long]("version")
        registeredQuorumSets <- c.get[Map[NodeID, QuorumSet]]("registeredQuorumSets")
        hash <- c.get[Hash]("hash")
      } yield QuorumSetSync(version, registeredQuorumSets, hash)
    }
  }
}
