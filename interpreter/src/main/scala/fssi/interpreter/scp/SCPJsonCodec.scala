package fssi
package interpreter
package scp

import utils._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import fssi.types._
import fssi.types.json.implicits._

import bigknife.scalap.ast.types.Message._
import bigknife.scalap.ast.types.{Signature => SCPSignature, Hash => SCPHash, _}

//import scala.collection._
import scala.collection.immutable

trait SCPJsonCodec extends HandlerCommons {
  implicit val valueJsonEncoder: Encoder[Value] = {
    case BlockValue(block, _) => block.asJson
    case a                    => a.asHex().asJson
  }

  // only support MomentValue
  implicit val valueJsonDecoder: Decoder[Value] = (a: HCursor) => {
    for {
      block <- a.value.as[Block]
    } yield {
      BlockValue(block, calculateTotalBlockBytes(block))
    }
  }

  implicit val valueSetJsonEncoder: Encoder[ValueSet] = (a: ValueSet) => a.toVector.asJson

  implicit val valueSetJsonDecoder: Decoder[ValueSet] = (c: HCursor) =>
    for {
      vec <- c.value.as[Vector[Value]]
    } yield ValueSet(vec: _*)

  implicit val messageJsonEncoder: Encoder[Message] = {
    case x: Nomination  => x.asJson
    case x: Prepare     => x.asJson
    case x: Commit      => x.asJson
    case x: Externalize => x.asJson
  }

  implicit val messageNominationDecoder: Decoder[Message.Nomination] = (c: HCursor) =>
    for {
      voted    <- c.get[ValueSet]("voted")
      accepted <- c.get[ValueSet]("accepted")
    } yield Message.Nomination(voted, accepted)

  implicit val ballotJsonDecoder: Decoder[Ballot] = (c: HCursor) => {
    val counter = c.get[Int]("counter")
    counter.flatMap {
      case x if x == 0 => Right(Ballot.Null)
      case x =>
        for {
          v <- c.get[Value]("value")
        } yield Ballot(x, v)
    }
  }

  implicit val messagePrepareDecoder: Decoder[Message.Prepare] = (c: HCursor) =>
    for {
      ballot        <- c.get[Ballot]("ballot")
      prepared      <- c.get[Ballot]("prepared")
      preparedPrime <- c.get[Ballot]("preparedPrime")
      hCounter      <- c.get[Int]("hCounter")
      cCounter      <- c.get[Int]("cCounter")
    } yield Message.Prepare(ballot, prepared, preparedPrime, hCounter, cCounter)

  implicit val messageCommitDecoder: Decoder[Message.Commit] = (c: HCursor) =>
    for {
      ballot          <- c.get[Ballot]("ballot")
      preparedCounter <- c.get[Int]("preparedCounter")
      hCounter        <- c.get[Int]("hCounter")
      cCounter        <- c.get[Int]("cCounter")
    } yield Message.Commit(ballot, preparedCounter, hCounter, cCounter)

  implicit val messageExternalizeDecoder: Decoder[Message.Externalize] = (c: HCursor) =>
    for {
      ballot   <- c.get[Ballot]("commit")
      hCounter <- c.get[Int]("hCounter")
    } yield Message.Externalize(ballot, hCounter)

  implicit val nodeIDJsonEncoder: Encoder[NodeID] = (a: NodeID) => a.asHex().asJson

  implicit val nodeIDJsonDecoder: Decoder[NodeID] = (c: HCursor) =>
    for {
      hex <- c.as[String]
    } yield NodeID(BytesValue.decodeHex(hex).bytes)

  implicit val slotIndexJsonEncoder: Encoder[SlotIndex] = (a: SlotIndex) => a.asHex().asJson

  implicit val slotIndexJsonDecoder: Decoder[SlotIndex] = (c: HCursor) =>
    for {
      hex <- c.as[String]
    } yield SlotIndex(BigInt(BytesValue.decodeHex(hex).bytes))

  implicit val scpHashJsonEncoder: Encoder[SCPHash] = (a: SCPHash) => a.asHex().asJson

  implicit val scpHashJsonDecoder: Decoder[SCPHash] = (c: HCursor) =>
    for {
      hex <- c.as[String]
    } yield SCPHash(BytesValue.decodeHex(hex).bytes)

  implicit def statementJsonEncoder[M <: Message]: Encoder[Statement[M]] = {
    case x: Statement.Nominate    => x.asJson
    case x: Statement.Commit      => x.asJson
    case x: Statement.Externalize => x.asJson
    case x: Statement.Prepare     => x.asJson
  }

  implicit val signatureJsonEncoder: Encoder[SCPSignature] = (a: SCPSignature) => a.asHex().asJson

  implicit val signatureJsonDecoder: Decoder[SCPSignature] = (c: HCursor) =>
    for {
      x <- c.value.as[String]
    } yield SCPSignature(BytesValue.decodeHex(x).bytes)

  implicit def envelopeJsonEncoder[M <: Message]: Encoder[Envelope[M]] =
    (a: Envelope[M]) =>
      a match {
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

  implicit val statementNominateJsonDecoder: Decoder[Statement.Nominate] = (c: HCursor) => {
    for {
      nodeID        <- c.get[NodeID]("nodeID")
      slotIndex     <- c.get[SlotIndex]("slotIndex")
      quorumSetHash <- c.get[SCPHash]("quorumSetHash")
      message       <- c.get[Message.Nomination]("message")
    } yield Statement.Nominate(nodeID, slotIndex, quorumSetHash, message)
  }

  implicit val statementCommitJsonDecoder: Decoder[Statement.Commit] = (c: HCursor) => {
    for {
      nodeID        <- c.get[NodeID]("nodeID")
      slotIndex     <- c.get[SlotIndex]("slotIndex")
      quorumSetHash <- c.get[SCPHash]("quorumSetHash")
      message       <- c.get[Message.Commit]("message")
    } yield Statement.Commit(nodeID, slotIndex, quorumSetHash, message)
  }

  implicit val statementPrepareJsonDecoder: Decoder[Statement.Prepare] = (c: HCursor) => {
    for {
      nodeID        <- c.get[NodeID]("nodeID")
      slotIndex     <- c.get[SlotIndex]("slotIndex")
      quorumSetHash <- c.get[SCPHash]("quorumSetHash")
      message       <- c.get[Message.Prepare]("message")
    } yield Statement.Prepare(nodeID, slotIndex, quorumSetHash, message)
  }

  implicit val statementExternalizeJsonDecoder: Decoder[Statement.Externalize] =
    (c: HCursor) => {
      for {
        nodeID        <- c.get[NodeID]("nodeID")
        slotIndex     <- c.get[SlotIndex]("slotIndex")
        quorumSetHash <- c.get[SCPHash]("quorumSetHash")
        message       <- c.get[Message.Externalize]("message")
      } yield Statement.Externalize(nodeID, slotIndex, quorumSetHash, message)
    }

  implicit val envelopeJsonDecoder: Decoder[Envelope[Message]] = (c: HCursor) => {
    c.get[String]("type").flatMap {
      case "nominate" =>
        for {
          st  <- c.get[Statement.Nominate]("statement")
          sig <- c.get[SCPSignature]("signature")
        } yield Envelope.NominationEnvelope(st, sig)

      case "prepare" =>
        for {
          st  <- c.get[Statement.Prepare]("statement")
          sig <- c.get[SCPSignature]("signature")
        } yield Envelope.BallotEnvelope(st, sig)

      case "commit" =>
        for {
          st  <- c.get[Statement.Commit]("statement")
          sig <- c.get[SCPSignature]("signature")
        } yield Envelope.BallotEnvelope(st, sig)

      case "externalize" =>
        for {
          st  <- c.get[Statement.Externalize]("statement")
          sig <- c.get[SCPSignature]("signature")
        } yield Envelope.BallotEnvelope(st, sig)
    }
  }

  implicit val quorumSetJsonEncoder: Encoder[QuorumSet] = {
    case x @ QuorumSet.Simple(threashold, validators)         => x.asJson
    case x @ QuorumSet.Nest(threshold, validators, innerSets) => x.asJson
  }

  implicit val quorumSetJsonDecoder: Decoder[QuorumSet] =
    (c: HCursor) => {
      c.get[Set[QuorumSet.Simple]]("innerSets") match {
        case Left(_) =>
          for {
            threshold  <- c.get[Int]("threshold")
            validators <- c.get[Set[NodeID]]("validators")
          } yield QuorumSet.Simple(threshold, validators)

        case Right(innerSets) =>
          for {
            threshold  <- c.get[Int]("threshold")
            validators <- c.get[Set[NodeID]]("validators")
          } yield QuorumSet.Nest(threshold, validators, innerSets)
      }
    }

  implicit val quorumSetsJsonEncoder: Encoder[Map[NodeID, QuorumSet]] =
    (a: Map[NodeID, QuorumSet]) =>
      Json.fromValues(a.toVector.map {
        case (nodeID, quorumSet) =>
          Json.obj(
            "nodeID" -> nodeID.asJson,
            "qs"     -> quorumSet.asJson
          )
      })

  implicit val quorumSetsJsonDecoder: Decoder[Map[NodeID, QuorumSet]] = (c: HCursor) => {
    val s: immutable.Seq[Either[DecodingFailure, (NodeID, QuorumSet)]] =
      c.value.asArray.get.map { json =>
        val jso = json.asObject.get
        for {
          nodeID <- jso("nodeID").get.as[NodeID]
          qs     <- jso("qs").get.as[QuorumSet]
        } yield nodeID -> qs
      }
    Right(s.map(_.right.get).toMap)
  }

  implicit val quorumSetSyncJsonEncoder: Encoder[QuorumSetSync] = (a: QuorumSetSync) =>
    Json.obj(
      "version"              -> Json.fromLong(a.version),
      "registeredQuorumSets" -> a.registeredQuorumSets.asJson,
      "hash"                 -> a.hash.asJson
  )

  implicit val quorumSetSyncJsonDecoder: Decoder[QuorumSetSync] = (c: HCursor) => {
    for {
      version              <- c.get[Long]("version")
      registeredQuorumSets <- c.get[Map[NodeID, QuorumSet]]("registeredQuorumSets")
      hash                 <- c.get[SCPHash]("hash")
    } yield QuorumSetSync(version, registeredQuorumSets, hash)
  }

}
