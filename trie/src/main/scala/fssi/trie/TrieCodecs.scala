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

  /** Node.Slot json encoder */
  implicit def trieSlotNodeJsonEncoder[K, V](implicit K: Encoder[K],
                                             V: Encoder[V],
                                             BK: Bytes[K],
                                             BV: Bytes[V]): Encoder[Slot[K, V]] =
    (x: Slot[K, V]) => {
      Json.obj("Slot" -> Json.obj(
                 "idx"  -> x.idx.asJson,
                 "data" -> x.data.asJson,
                 "node" -> x.node.asJson
               ),
               "hash" -> Json.fromString(x.hexHash))
    }

  /** Node.Slot json decoder */
  implicit def trieSlotNodeJsonDecoder[K: ClassTag, V](implicit K: Decoder[K],
                                                       V: Decoder[V],
                                                       BK: Bytes[K],
                                                       BV: Bytes[V]): Decoder[Slot[K, V]] =
    (a: HCursor) => {
      val hashCursor = a.downField("hash")
      val slotCursor = a.downField("Slot")
      if (hashCursor.failed)
        Left(DecodingFailure("Slot's hash field not found", List(CursorOp.DownField("hash"))))
      else if (slotCursor.failed)
        Left(DecodingFailure("Slot's Slot field not found", List(CursorOp.DownField("Slot"))))
      else {
        val slotResult = for {
          idx  <- slotCursor.get[K]("idx")
          data <- slotCursor.get[Option[V]]("data")
          node <- slotCursor.get[Option[Node[K, V]]]("node")
        } yield Slot(idx, data, node)
        val hashResult = hashCursor.as[String]
        for {
          slot <- slotResult
          hash <- hashResult
          verified <- if (BytesUtil.decodeHex(hash) sameElements slot.hash) Right(slot)
          else Left(DecodingFailure("Slot hash verifying failed", List(CursorOp.Field("hash"))))
        } yield verified
      }
    }

  /** Node.Compact json decoder */
  implicit def trieCompactNodeJsonEncoder[K, V](implicit K: Encoder[K],
                                                V: Encoder[V],
                                                BK: Bytes[K],
                                                BV: Bytes[V]): Encoder[Compact[K, V]] =
    (x: Compact[K, V]) =>
      Json.obj("Compact" -> Json.obj(
                 "indexes" -> x.indexes.asJson,
                 "data"    -> x.data.asJson,
                 "node"    -> x.node.asJson
               ),
               "hash" -> Json.fromString(x.hexHash))

  /** Node.Compact json decoder */
  implicit def trieCompactNodeJsonDecoder[K: ClassTag, V](implicit K: Decoder[K],
                                                          V: Decoder[V],
                                                          BK: Bytes[K],
                                                          BV: Bytes[V]): Decoder[Compact[K, V]] =
    (a: HCursor) => {
      val hashCursor    = a.downField("hash")
      val compactCursor = a.downField("Compact")
      if (hashCursor.failed)
        Left(DecodingFailure("Compact's hash field not found", List(CursorOp.DownField("hash"))))
      else if (compactCursor.failed)
        Left(
          DecodingFailure("Compact's Compact field not found", List(CursorOp.DownField("Compact"))))
      else {
        val compactResult = for {
          indexes <- compactCursor.get[Array[K]]("indexes")
          data    <- compactCursor.get[Option[V]]("data")
          node    <- compactCursor.get[Option[Node[K, V]]]("node")
        } yield Compact(indexes, data, node)
        val hashResult = hashCursor.as[String]
        for {
          compact <- compactResult
          hash    <- hashResult
          verified <- if (BytesUtil.decodeHex(hash) sameElements compact.hash) Right(compact)
          else Left(DecodingFailure("Compact hash verifying failed", List(CursorOp.Field("hash"))))
        } yield verified
      }
    }

  /** Node.Branch json decoder */
  implicit def trieBranchNodeJsonEncoder[K, V](implicit K: Encoder[K],
                                               V: Encoder[V],
                                               BK: Bytes[K],
                                               BV: Bytes[V]): Encoder[Branch[K, V]] =
    (x: Branch[K, V]) =>
      Json.obj("Branch" -> Json.obj("slots" -> x.slots.toVector.map(_._2).asJson),
               "hash"   -> Json.fromString(x.hexHash))

  /** Node.Compact json decoder */
  implicit def trieBranchNodeJsonDecoder[K: ClassTag, V](implicit K: Decoder[K],
                                                         V: Decoder[V],
                                                         BK: Bytes[K],
                                                         BV: Bytes[V]): Decoder[Branch[K, V]] =
    (a: HCursor) => {
      val hashCursor   = a.downField("hash")
      val branchCursor = a.downField("Branch")
      if (hashCursor.failed)
        Left(DecodingFailure("Branch's hash field not found", List(CursorOp.DownField("hash"))))
      else if (branchCursor.failed)
        Left(
          DecodingFailure("Branch's Compact field not found", List(CursorOp.DownField("Compact"))))
      else {
        val branchResult = for {
          slots <- branchCursor.get[Vector[Slot[K, V]]]("slots")
        } yield Branch(slots.map(x => x.idx -> x).toMap)
        val hashResult = hashCursor.as[String]
        for {
          compact <- branchResult
          hash    <- hashResult
          verified <- if (BytesUtil.decodeHex(hash) sameElements compact.hash) Right(compact)
          else Left(DecodingFailure("Branch hash verifying failed", List(CursorOp.Field("hash"))))
        } yield verified
      }
    }

  implicit def trieJsonEncoder[K, V](implicit K: Encoder[K],
                                     V: Encoder[V],
                                     BK: Bytes[K],
                                     BV: Bytes[V]): Encoder[Trie[K, V]] = {
    case x @ FssiTrie(root) =>
      Json.obj(
        "FssiTrie" ->
          Json.obj(
            "root" -> root.asJson,
            "hash" -> x.rootHexHash.asJson
          ))
  }

  implicit def trieJsonDecoder[K: ClassTag, V](implicit K: Decoder[K],
                                               V: Decoder[V],
                                               BK: Bytes[K],
                                               BV: Bytes[V]): Decoder[Trie[K, V]] =
    (a: HCursor) => {
      val fssiTrie = a.downField("FssiTrie")
      if (fssiTrie.succeeded) {
        for {
          root <- fssiTrie.get[Option[Node[K, V]]]("root")
          trie = FssiTrie(root)
          hash <- fssiTrie.get[String]("hash")
          verified <- if (BytesUtil.decodeHex(hash) sameElements trie.rootHash) Right(trie)
          else Left(DecodingFailure("FssiTrie hash verifying failed", List(CursorOp.Field("hash"))))
        } yield verified
      } else Left(DecodingFailure("FssiTrie field not found", List(CursorOp.Field("FssiTrie"))))
    }

}
