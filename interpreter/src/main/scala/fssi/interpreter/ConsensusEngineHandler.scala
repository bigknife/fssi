package fssi
package interpreter

import types._
import exception._
import ast._
import org.slf4j._
import fssi.interpreter.scp.{QuorumSetSyncMessage, SCPEnvelopeMessage}
import fssi.scp.ast.uc.SCP
import fssi.scp.types._
import fssi.scp.ast.components._
import fssi.scp.interpreter.{runner => scpRunner}

class ConsensusEngineHandler
    extends ConsensusEngine.Handler[Stack]
    with BlockCalSupport
    with SCPSupport
    with LogSupport {

  private val scp: SCP[Model.Op] = SCP[Model.Op]

  /** initialize consensus engine
    */
  override def initializeConsensusEngine(account: Account): Stack[Unit] = Stack { setting =>
    // only core node need run consensus
    setting match {
      case x: Setting.CoreNodeSetting =>
        val scpSetting = unsafeResolveSCPSetting(account, x)

        //scpRunner.runIO(scp.initialize(), scpSetting).unsafeRunSync

      case _ => // nothing to do
    }
  }

  /** try to agree a new block
    * @param account the consensus procedure initiator
    * @param previous the previous block, latest determined block
    * @param agreeing current block, being in consensus procedure
    */
  override def tryToAgreeBlock(account: Account, previous: Block, agreeing: Block): Stack[Unit] =
    Stack { setting =>
      setting match {
        case x: Setting.CoreNodeSetting =>
          val nodeID        = NodeID(account.id.value.bytes)
          val value         = BlockValue(agreeing, calculateTotalBlockBytes(agreeing))
          val previousValue = BlockValue(previous, calculateTotalBlockBytes(previous))
          val slotIndex     = SlotIndex(agreeing.height)
          val p = scp.nominate(nodeID,
                               slotIndex,
                               round = 0,
                               valueToNominate = value,
                               previousValue = previousValue)

          // invoke scp to nominate `agreeing`
          val scpSetting = unsafeResolveSCPSetting(account, x)
          val r          = bigknife.scalap.interpreter.runner.runIO(p, scpSetting).unsafeRunSync()
          log.info(s"run scp nominate program: $r")
          ()

        case _ => //nothhing todo
      }
    }

  /** handle consensus-special message
    */
  override def handleConsensusAuxMessage(account: Account,
                                         auxMessage: ConsensusAuxMessage): Stack[Unit] = Stack {
    setting =>
      setting match {
        case x: Setting.CoreNodeSetting =>
          val scpSetting = unsafeResolveSCPSetting(account, x)
          auxMessage match {
            case QuorumSetSyncMessage(quorumSetSync) =>
              quorumSetSync.registeredQuorumSets.foreach {
                case (nodeId, qss) =>
                  log.debug(s"handle consensus quorum set sync message: ($nodeId -> $qss)")
                  scpRunner.runIO(scp.cacheQuorumSet(qss), scpSetting).unsafeRunSync
              }
            case SCPEnvelopeMessage(envelope) =>
              log.debug(s"handling scp envelope: $envelope")
              scpRunner
                .runIO(scp.processEnvelope(NodeID(account.publicKey.bytes), envelope),
                       scpSetting)
                .unsafeRunSync
              log.debug(s"handled scp envelope: $envelope")
            case _ => //impossible
          }

        case _ => //nothing to do
      }

  }
}

object ConsensusEngineHandler {
  private val instance: ConsensusEngineHandler = new ConsensusEngineHandler

  trait Implicits {
    implicit val consensusEngineHandler: ConsensusEngineHandler = instance
  }
}
