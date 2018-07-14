package fssi.interpreter

import fssi.ast.domain.LedgerService
import fssi.ast.domain.types.{BytesValue, Hash, Moment, TimeCapsule}

class LedgerServiceHandler extends LedgerService.Handler[Stack] {
  override def createTimeCapsule(height: BigInt,
                                 previousHash: Hash,
                                 moments: Vector[Moment]): Stack[TimeCapsule] = Stack {
    hashTimeCapsule(
      TimeCapsule(
        height,
        moments,
        Hash.empty,
        previousHash
      ))
  }

  override def createGeniusTimeCapsule(): Stack[TimeCapsule] = Stack {
    val empty = TimeCapsule(BigInt(0), Vector.empty, Hash.empty, Hash.empty)
    hashTimeCapsule(empty)
  }

  private def hashTimeCapsule(tc: TimeCapsule): TimeCapsule = {
    import io.circe.syntax._
    import fssi.interpreter.jsonCodec._

    val magic = "modeerF".getBytes

    val toHash = tc.height.toByteArray ++ tc.moments
      .map(_.asJson.noSpaces)
      .mkString("")
      .getBytes ++ tc.previousHash.bytes ++ magic

    val hash = Hash(fssi.interpreter.util.crypto.hash(BytesValue(toHash)).bytes)
    tc.copy(hash = hash)
  }
}

object LedgerServiceHandler {
  private val _instance: LedgerServiceHandler = new LedgerServiceHandler

  trait Implicits {
    implicit val ledgerServiceHandler: LedgerServiceHandler = _instance
  }

}
