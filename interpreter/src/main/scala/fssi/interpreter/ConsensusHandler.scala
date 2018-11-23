package fssi
package interpreter
import fssi.ast.Consensus
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.interpreter.scp.{BlockValue, SCPEnvelope, SCPSupport}
import fssi.scp._
import fssi.scp.types.SlotIndex
import fssi.types.{ConsensusMessage, ReceiptSet, TransactionSet}
import fssi.types.base.{Hash, Timestamp, WorldState}
import fssi.types.biz.Node.ConsensusNode
import fssi.types.biz.{Block, Receipt, Transaction}
import utils._

class ConsensusHandler
    extends Consensus.Handler[Stack]
    with SCPSupport
    with UnsignedBytesSupport
    with LogSupport {

  override def initialize(node: ConsensusNode, currentHeight: BigInt): Stack[Unit] = Stack {
    setting =>
      setting match {
        case coreNodeSetting: CoreNodeSetting =>
          implicit val scpSetting: fssi.scp.interpreter.Setting = resolveSCPSetting(coreNodeSetting)
          Portal.initialize(SlotIndex(currentHeight + 1))
        case _ =>
      }
  }

  override def destroy(): Stack[Unit] = Stack {}

  override def tryAgree(transaction: Transaction,
                        receipt: Receipt,
                        lastDeterminedBlock: Block,
                        currentWorldState: WorldState): Stack[Unit] = Stack { setting =>
    setting match {
      case coreNodeSetting: CoreNodeSetting =>
        implicit val scpSetting: fssi.scp.interpreter.Setting = resolveSCPSetting(coreNodeSetting)
        val nodeId                                            = scpSetting.localNode
        val chainId                                           = coreNodeSetting.config.chainId
        val height                                            = lastDeterminedBlock.height + 1
        val slotIndex                                         = SlotIndex(height)
        val preWorldState                                     = lastDeterminedBlock.curWorldState
        val transactions                                      = TransactionSet(transaction)
        val receipts                                          = ReceiptSet(receipt)
        val timestamp                                         = Timestamp(System.currentTimeMillis())
        val block = Block(height,
                          chainId,
                          preWorldState,
                          currentWorldState,
                          transactions,
                          receipts,
                          timestamp,
                          Hash.empty)
        val hash          = Hash(crypto.hash(calculateUnsignedBlockBytes(block)))
        val blockValue    = BlockValue(block.copy(hash = hash))
        val previousValue = BlockValue(lastDeterminedBlock)
        Portal.handleRequest(nodeId, slotIndex, previousValue, blockValue)
        log.debug(s"try to agree block value: $blockValue , previousValue: $previousValue")
      case _ =>
    }
  }

  override def processMessage(message: ConsensusMessage, lastDeterminedBlock: Block): Stack[Unit] =
    Stack { setting =>
      setting match {
        case coreNodeSetting: CoreNodeSetting =>
          message match {
            case SCPEnvelope(envelope) =>
              log.debug(s"handling scp envelope: $envelope")
              implicit val scpSetting: fssi.scp.interpreter.Setting =
                resolveSCPSetting(coreNodeSetting)
              val previousValue = BlockValue(lastDeterminedBlock)
              Portal.handleEnvelope(envelope, previousValue)
              log.debug(s"handled scp envelope: $envelope")
          }
        case _ =>
      }
    }
}

object ConsensusHandler {
  val instance = new ConsensusHandler

  trait Implicits {
    implicit val consensusHandler: ConsensusHandler = instance
  }
}
