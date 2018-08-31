package fssi
package corenode
package handler

import types._
import interpreter._
import scp._
import types.syntax._
import ast._
import bigknife.scalap.ast.types.{Envelope, Message}
import bigknife.scalap.ast.usecase.{component => scpcomponent, _}
import bigknife.scalap.interpreter.{runner => scprunner}
import uc._
import utils._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import interpreter.jsonCodecs._
import org.slf4j._

trait StartupHandler extends JsonMessageHandler {
  private val log = LoggerFactory.getLogger(getClass)

  private val coreNodeProgram                            = CoreNodeProgram[components.Model.Op]
  private val scpProgram = SCP[scpcomponent.Model.Op]
  private val settingOnce: Once[Setting.CoreNodeSetting] = Once.empty

  // message type names can be handled
  private val acceptedTypeNames: Vector[String] = Vector(
    JsonMessage.TYPE_NAME_TRANSACTION, // then, the body will be parsed to a Transaction object
    JsonMessage.TYPE_NAME_QUORUMSET_SYNC, // then, the body will be parsed to a QuorumSetSync handler
    JsonMessage.TYPE_NAME_SCP, // then, the body will be parsed to a SCPEnvelope
  )

  def apply(setting: Setting.CoreNodeSetting): Unit = {
    val node =
      runner.runIO(coreNodeProgram.startup(setting.workingDir, this), setting).unsafeRunSync
    settingOnce := setting

    // add shutdown hook to clean resources.
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      runner.runIO(coreNodeProgram.shutdown(node), setting).unsafeRunSync
    }))

    // long running.
    Thread.currentThread.join()
  }

  def showNode(node: Node): String = node.toString

  def ignored(message: JsonMessage): Boolean = !acceptedTypeNames.contains(message.typeName)

  def handle(jsonMessage: JsonMessage): Unit = {
    import JsonMessage._
    jsonMessage.typeName match {
      case TYPE_NAME_TRANSACTION =>
        val transactionResult = for {
          json        <- parse(jsonMessage.body)
          transaction <- json.as[Transaction]
        } yield transaction

        transactionResult match {
          case Left(t)            => log.error("transaction json deserialization faield", t)
          case Right(transaction) =>
            log.debug(s"start handle transaction: $transaction")
            settingOnce foreach { setting =>
              runner.runIO(coreNodeProgram.handleTransaction(transaction), setting).unsafeRunSync
            }
            log.debug(s"handled transaction: ${transaction.id}")

        }

      case TYPE_NAME_QUORUMSET_SYNC =>
        val quorumSyncResult = for {
          json <- parse(jsonMessage.body)
          quorumSync <- json.as[QuorumSetSync]
        } yield quorumSync
        quorumSyncResult match {
          case Left(t) => log.error("quorum set sync json deserialization failed", t)
          case Right(qss) =>
            log.debug(s"start handle quorum set sync: $qss")
            settingOnce.foreach { setting =>
              runner.runIO(coreNodeProgram.handleConsensusAuxMessage(QuorumSetSyncMessage(qss)),setting).unsafeRunSync()
            }
            log.debug(s"handled quorum set sync: $qss")
        }

      case TYPE_NAME_SCP =>
        val scpEnvelopeResult = for {
          json <- parse(jsonMessage.body)
          envelope <- json.as[Envelope[Message]]
        } yield envelope

        scpEnvelopeResult match {
          case Left(t) => log.error("scp envelope json deserialization failed", t)
          case Right(envelope) =>
            log.debug(s"start handle scp envelope: $envelope")
            settingOnce.foreach {setting =>
              runner.runIO(coreNodeProgram.handleConsensusAuxMessage(SCPEnvelopeMessage(envelope)), setting).unsafeRunSync
            }
            log.debug(s"handled scp envelope: $envelope")
        }


      case x => throw new RuntimeException(s"Unsupport TypeName: $x")
    }
  }

}
