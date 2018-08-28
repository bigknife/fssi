package fssi
package trie

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import Trie._
import Node._
import fssi.utils.BytesUtil

import scala.reflect.ClassTag

trait TrieCodecs {
  implicit def slotJsonEncoder[K, V](implicit K: Encoder[K],
                                     V: Encoder[V],
                                     BK: Bytes[K],
                                     BV: Bytes[V]): Encoder[Slot[K, V]] = (x: Slot[K, V]) => {
    Json.obj("Slot" -> Json.obj(
               "idx"  -> x.idx.asJson,
               "data" -> x.data.asJson,
               "node" -> x.node.asJson
             ),
             "hash" -> Json.fromString(x.hexHash))
  }
  implicit def nodeJsonEncoder[K, V](implicit K: Encoder[K],
                                     V: Encoder[V],
                                     BK: Bytes[K],
                                     BV: Bytes[V]): Encoder[Node[K, V]] = {
    case x @ Slot(idx, data, node) => x.asJson
    case x @ Compact(indexes, data, node) =>
      Json.obj("Compact" -> Json.obj(
                 "indexes" -> indexes.asJson,
                 "data"    -> data.asJson,
                 "node"    -> node.asJson
               ),
               "hash" -> Json.fromString(x.hexHash))
    case x @ Branch(slots) =>
      Json.obj("Branch" -> Json.obj("slots" -> slots.toVector.map(_._2).asJson),
               "hash"   -> Json.fromString(x.hexHash))
  }

  implicit def trieJsonEncoder[K, V](implicit K: Encoder[K],
                                     V: Encoder[V],
                                     BK: Bytes[K],
                                     BV: Bytes[V]): Encoder[Trie[K, V]] = {
    case x @ SimpleTrie(root) =>
      Json.obj(
        "root" -> root.asJson,
        "hash" -> x.rootHexHash.asJson
      )
  }

  implicit def nodeJsonDecoder[K: ClassTag, V](implicit K: Decoder[K],
                                     V: Decoder[V],
                                     BK: Bytes[K],
                                     BV: Bytes[V]): Decoder[Node[K, V]] =
    (a: HCursor) => {
      val sd = a.downField("Slot")
      val cd = a.downField("Compact")
      val bd = a.downField("Branch")
      if (sd.succeeded) {
        for {
          idx <- sd.get[K]("idx")
          data <- sd.get[Option[V]]("data")
          node <- sd.get[Option[Node[K, V]]]("node")
          simpleData = Slot(idx, data, node)
          hash <- a.get[String]("hash")
          r <- if (simpleData.hash sameElements BytesUtil.decodeHex(hash)) Right(simpleData)
          else Left(DecodingFailure("Slot Hash Not Consistent", List()))
        } yield r
      } else if (cd.succeeded) {
        for {
          indexes <- cd.get[Array[K]]("indexes")
          data    <- cd.get[Option[V]]("data")
          node <- cd.get[Option[Node[K, V]]]("node")
          compactData = Compact(indexes, data, node)
          hash <- cd.get[String]("hash")
          r <- if (compactData.hash sameElements BytesUtil.decodeHex(hash)) Right(compactData)
          else Left(DecodingFailure("Compact Hash Not Consistent", List()))
        } yield r
      } else if (bd.succeeded) {
        for {
          slots <- bd.get[Vector[Slot[K, V]]]("slots")
          branch = Branch(slots.map(x => x.idx -> x).toMap)
          hash <- bd.get[String]("hash")
          r <- if (branch.hash sameElements BytesUtil.decodeHex(hash)) Right(branch)
          else Left(DecodingFailure("Branch Hash Not Consistent", List()))
        } yield r
      } else Left(DecodingFailure("Can't found 'SimpleData' or 'CompactData' or 'Branch'", List()))
    }

  implicit def nodeJsonDecoder[K: ClassTag, V](implicit K: Decoder[K],
                                               V: Decoder[V],
                                               BK: Bytes[K],
                                               BV: Bytes[V]): Decoder[Trie[K, V]] = (a: HCursor) => {
    for {
      root <- a.get[Option[Node[K, V]]]("root")
      st = SimpleTrie(root)
      hash <- a.get[String]("hash")
    } yield st
  }
  /*

  implicit def trieJsonEncoder[A](implicit E: Encoder[A]): Encoder[Trie[A]] = {
    case x @ SimpleTrie(root) =>
      Json.obj(
        "root" -> root.asJson,
        "hash" -> x.rootHexHash.asJson
      )
  }
 */
}
