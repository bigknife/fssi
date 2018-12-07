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

      nominateMessage0 <- nominateEnvelope(slotIndex)
      _ <- ifThen(nominateMessage0.nonEmpty) {
        for {
          _ <- broadcastEnvelope(slotIndex, nominateMessage0.get)
          _ <- infoSentEnvelope(nominateMessage0.get)
        } yield ()
      }

      nominateMessage1 <- nominateEnvelope(nextSlotIndex)
      _ <- ifThen(nominateMessage1.nonEmpty) {
        for {
          _ <- broadcastEnvelope(nextSlotIndex, nominateMessage1.get)
          _ <- infoSentEnvelope(nominateMessage1.get)
        } yield ()
      }

      ballotMessage0 <- ballotEnvelope(slotIndex)
      _ <- ifThen(ballotMessage0.nonEmpty) {
        for {
          _ <- broadcastEnvelope(slotIndex, ballotMessage0.get)
          _ <- infoSentEnvelope(ballotMessage0.get)
        } yield ()
      }

      ballotMessage1 <- ballotEnvelope(nextSlotIndex)
      _ <- ifThen(ballotMessage1.nonEmpty) {
        for {
          _ <- broadcastEnvelope(nextSlotIndex, ballotMessage1.get)
          _ <- infoSentEnvelope(ballotMessage1.get)
        } yield ()
      }
      _ <- delayExecuteProgram(BROADCAST_TIMER, broadcastMessageRegularly(), timeout)
    } yield ()
  }
}
