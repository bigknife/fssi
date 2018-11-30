package fssi
package scp
package ast
package uc
import types._
import bigknife.sop._
import bigknife.sop.implicits._

trait BroadcastMessageProgram[F[_]] extends SCP[F] with BaseProgram[F] {
  import model.nodeStore._
  import model.nodeService._
  import model.applicationService._
  import model.logService._

  /** broadcast nominate and ballot message until externalized
    */
  def broadcastMessageRegularly(): SP[F, Unit] = {
    for {
      slotIndex <- currentSlotIndex()
      nextSlotIndex = slotIndex + 1
      timeout         <- broadcastTimeout()
      _               <- debug(s"broadcast $nextSlotIndex message regularly,timeout: $timeout millis")
      nominateMessage <- nominateEnvelope(nextSlotIndex)
      _ <- ifThen(nominateMessage.nonEmpty) {
        for {
          _ <- broadcastEnvelope(nextSlotIndex, nominateMessage.get)
          _ <- infoSentEnvelope(nominateMessage.get)
        } yield ()
      }

      ballotMessage <- ballotEnvelope(nextSlotIndex)
      _ <- ifThen(ballotMessage.nonEmpty) {
        for {
          _ <- broadcastEnvelope(nextSlotIndex, ballotMessage.get)
          _ <- infoSentEnvelope(ballotMessage.get)
        } yield ()
      }
      _ <- delayExecuteProgram(BROADCAST_TIMER, broadcastMessageRegularly(), timeout)
    } yield ()
  }
}
