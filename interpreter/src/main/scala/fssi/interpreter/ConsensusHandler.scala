package fssi
package interpreter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import bigknife.sop.SP
import fssi.ast.Consensus
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.interpreter.scp.{BlockValue, SCPEnvelope, SCPSupport}
import fssi.scp._
import fssi.scp.types.SlotIndex
import fssi.types.base.{Hash, Timestamp, WorldState}
import fssi.types.biz.Block
import fssi.types.biz.Node.ConsensusNode
import fssi.types.{ConsensusMessage, ReceiptSet, TransactionSet}
import fssi.utils._

class ConsensusHandler
    extends Consensus.Handler[Stack]
    with SCPSupport
    with UnsignedBytesSupport
    with LogSupport {

  lazy val consensusLasting = new AtomicBoolean(false)

  override def initialize(node: ConsensusNode, currentHeight: BigInt): Stack[Unit] = Stack {
    setting =>
      setting match {
        case coreNodeSetting: CoreNodeSetting =>
          implicit val scpSetting: fssi.scp.interpreter.Setting = resolveSCPSetting(coreNodeSetting)
          Portal.initialize(SlotIndex(currentHeight))
        case _ =>
      }
  }

  override def destroy(): Stack[Unit] = Stack {}

  override def isConsensusLasting(): Stack[Boolean] = Stack {
    val isLasting = consensusLasting.get()
    isLasting
  }

  override def agreeTransactions(transactions: TransactionSet): Stack[Unit] = Stack { setting =>
    setting match {
      case coreNodeSetting: CoreNodeSetting =>
        implicit val scpSetting: fssi.scp.interpreter.Setting = resolveSCPSetting(coreNodeSetting)
        val nodeId                                            = scpSetting.localNode
        val chainId                                           = coreNodeSetting.config.chainId
        val lastDeterminedBlock =
          StoreHandler.instance.getLatestDeterminedBlock()(coreNodeSetting).unsafeRunSync()
        val height        = lastDeterminedBlock.height + 1
        val slotIndex     = SlotIndex(height)
        val preWorldState = lastDeterminedBlock.curWorldState
        val receipts      = ReceiptSet.empty
        val timestamp     = Timestamp(System.currentTimeMillis())
        val block = Block(height,
                          chainId,
                          preWorldState,
                          WorldState.empty,
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
            case x: SCPEnvelope =>
              implicit val scpSetting: fssi.scp.interpreter.Setting =
                resolveSCPSetting(coreNodeSetting)

              val previousValue = BlockValue(lastDeterminedBlock)
              Portal.handleEnvelope(x.value, previousValue)
          }
        case _ =>
      }
    }

  override def startConsensus(): Stack[Unit] = Stack {
    consensusLasting.set(true); ()
  }

  override def stopConsensus(): Stack[Unit] = Stack {
    consensusLasting.set(false); ()
  }

  private lazy val agreeExecutor = Executors.newSingleThreadExecutor()
  override def prepareExecuteAgreeProgram(program: Any): Stack[Unit] = Stack { setting =>
    val task: Runnable = () => {
      runner
        .runIO(program.asInstanceOf[SP[fssi.ast.blockchain.Model.Op, Unit]], setting)
        .unsafeRunSync()
    }
    agreeExecutor.submit(task); ()
  }

  override def subscribeExternalize(f: Block => Unit): Stack[Unit] = Stack {
    ConsensusHandler.listeners.updated(_ :+ f); ()
  }

  override def notifySubscriberWhenExternalized(block: Block): Stack[Unit] = Stack {
    ConsensusHandler.listeners.foreach(_.foreach(_(block)))
  }
}

object ConsensusHandler {
  val instance = new ConsensusHandler

  trait Implicits {
    implicit val consensusHandler: ConsensusHandler = instance
  }

  private lazy val listeners: SafeVar[Vector[Block => Unit]] = SafeVar(Vector.empty)
}
