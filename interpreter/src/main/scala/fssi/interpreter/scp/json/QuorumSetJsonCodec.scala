package fssi
package interpreter
package scp
package json

import bigknife.scalap.ast.types._

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._

trait QuorumSetJsonCodec {
  implicit val quorumSetJsonEncoder: Encoder[QuorumSet] = (a: QuorumSet) =>
    a match {
      case x: QuorumSet.Simple => x.asJson
      case x: QuorumSet.Nest   => x.asJson
  }

  implicit val quorumSetJsonDecoder: Decoder[QuorumSet] = (a: HCursor) => {
    a.get[Set[QuorumSet.Simple]]("innerSets") match {
      case Left(_) =>
        for {
          threshold  <- a.get[Int]("threshold")
          validators <- a.get[Set[NodeID]]("validators")
        } yield QuorumSet.Simple(threshold, validators)

      case Right(innerSets) =>
        for {
          threshold  <- a.get[Int]("threshold")
          validators <- a.get[Set[NodeID]]("validators")
        } yield QuorumSet.Nest(threshold, validators, innerSets)
    }
  }
}
