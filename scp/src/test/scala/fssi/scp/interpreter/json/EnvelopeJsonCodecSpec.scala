package fssi
package scp
package interpreter
package json
import fssi.scp.types.QuorumSet.{QuorumSlices, Slices}
import fssi.scp.types._
import fssi.utils.BytesUtil
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import implicits._
import org.scalatest.FunSuite
import io.circe.generic.auto._

import scala.collection.immutable.TreeSet

class EnvelopeJsonCodecSpec extends FunSuite {

  implicit val valueEncoder: Encoder[Value] = {
    case testValue: TestValue => testValue.asJson
  }
  implicit val valueDecoder: Decoder[Value] = (hCursor: HCursor) => hCursor.as[TestValue]

  val signature = Signature("signature".getBytes())

  val from                 = NodeID("from".getBytes())
  val slotIndex            = SlotIndex(BigInt(10))
  val timestamp            = Timestamp(1)
  val slices: Slices       = Slices.flat(1, from)
  val quorumSet: QuorumSet = QuorumSet.slices(slices)
  val testValue1           = TestValue(TreeSet(1, 2, 3, 4, 5))
  val testValue2           = TestValue(TreeSet(6, 7, 8, 9, 10))
  val voted                = ValueSet(testValue1, testValue2)
  val accepted             = ValueSet(testValue2, testValue1)
  val message: Message     = Message.Nomination(voted, accepted)
  val statement = Statement(from = from,
                            slotIndex = slotIndex,
                            timestamp = timestamp,
                            quorumSet = quorumSet,
                            message = message)
  val envelope: Envelope[Message] = Envelope(statement, signature)

  val jsonString: String = envelope.asJson.spaces2
  info(jsonString)

  val r = for {
    json <- parse(jsonString)
    res  <- json.as[Envelope[Message]]
  } yield res

  assert(r.isRight)
}
