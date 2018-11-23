package fssi.scp
package ast
package uc

import bigknife.sop._
import bigknife.sop.implicits._
import fssi.scp.types.Message.{BallotMessage, Nomination}
import fssi.scp.types._

trait EmitProgram[F[_]] extends SCP[F] with BaseProgram[F] {
  import model.applicationService._
  import model.logService._
  import model.nodeService._
  import model.nodeStore._

  def emitNomination(slotIndex: SlotIndex, previousValue: Value, message: Nomination): SP[F, Unit] =
    for {
      _        <- debug(s"[$slotIndex] try to emit message: $message")
      envelope <- putInEnvelope(slotIndex, message)
      _ <- ifThen(handleSCPEnvelope(envelope, previousValue)) {
        for {
          _        <- debug(s"[$slotIndex] message handled locally success")
          emitable <- canEmit(slotIndex, envelope)
          _ <- ifThen(emitable) {
            for {
              _ <- broadcastEnvelope(slotIndex, envelope)
              nominations <- nominationsReceived(slotIndex)
              _ <- info(s"[$slotIndex] emit nomination message to peers, now ${nominations.keys.size} ---> : ${nominations.keys}")
            } yield ()
          }
        } yield ()
      }
    } yield ()

  /** send message to let peer nodes know
    * first, let the envelope be processed locally
    * if processed, then broadcast to scp network.
    */
  def emitBallot(slotIndex: SlotIndex, previousValue: Value, message: BallotMessage): SP[F, Unit] =
    for {
      _             <- debug(s"[$slotIndex] message: $message is pending to be emitted")
      envelope      <- putInEnvelope(slotIndex, message)
      shouldProcess <- shouldProcess(slotIndex, envelope)
      _ <- ifThen(shouldProcess) {
        for {
          _ <- info(s"[$slotIndex] emitting message: $message should be processed ")
          _ <- ifThen(handleSCPEnvelope(envelope, previousValue)) {
            for {
              emitable <- canEmit(slotIndex, envelope)
              _ <- ifThen(emitable) {
                for {
                  _ <- info(
                    s"[$slotIndex] message: $message was handled successfully and then is to be broadcast")
                  _ <- envelopeReadyToBeEmitted(slotIndex, envelope)
                  _ <- broadcastEnvelope(slotIndex, envelope)
                } yield ()
              }
            } yield ()
          }
        } yield ()
      }
    } yield ()
}
