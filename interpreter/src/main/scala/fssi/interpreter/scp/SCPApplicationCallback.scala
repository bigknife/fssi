package fssi.interpreter.scp
import java.util.concurrent.{ExecutorService, Executors}

import fssi.ast.uc.CoreNodeProgram
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.interpreter.{LogSupport, NetworkHandler, UnsignedBytesSupport}
import fssi.scp.interpreter.ApplicationCallback
import fssi.scp.types._
import fssi.scp.types.implicits._
import fssi.types.base.Hash
import fssi.types.biz.Message.ConsensusMessage
import fssi.types.implicits._
import fssi.utils._
import fssi.interpreter._

class SCPApplicationCallback(coreNodeSetting: CoreNodeSetting)
    extends ApplicationCallback
    with UnsignedBytesSupport
    with LogSupport {

  override def validateValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Value.Validity =
    value match {
      case BlockValue(block) =>
        val hash = Hash(crypto.hash(calculateUnsignedBlockBytes(block)))
        if (hash === block.hash) {
          val existedBadSignature = block.transactions.exists { transaction =>
            val unsignedBytes = calculateUnsignedTransactionBytes(transaction)
            !crypto.verifySignature(
              transaction.signature.value,
              unsignedBytes,
              crypto.rebuildECPublicKey(transaction.publicKeyForVerifying.value, crypto.SECP256K1))
          }
          val existedGoodSignature = block.transactions.exists { transaction =>
            val unsignedBytes = calculateUnsignedTransactionBytes(transaction)
            crypto.verifySignature(
              transaction.signature.value,
              unsignedBytes,
              crypto.rebuildECPublicKey(transaction.publicKeyForVerifying.value, crypto.SECP256K1))
          }
          (existedBadSignature, existedGoodSignature) match {
            case (true, true)  => Value.Validity.MaybeValid
            case (false, true) => Value.Validity.FullyValidated
            case _             => Value.Validity.Invalid
          }
        } else Value.Validity.Invalid
    }

  override def combineValues(nodeId: NodeID,
                             slotIndex: SlotIndex,
                             value: ValueSet): Option[Value] = {
    if (value.isEmpty) None
    else {
      val newValue: Value = value.reduceLeft { (ac, value) =>
        (ac, value) match {
          case (BlockValue(acc), BlockValue(n)) =>
            if (acc.chainId == n.chainId && acc.height == n.height) {
              BlockValue(acc.copy(transactions = acc.transactions ++ n.transactions))
            } else
              throw new RuntimeException(
                s"scp value set were not inconsistent, found chainId(${acc.chainId},${n.chainId}) , height(${acc.height},${n.height})")
          case (acc, n) =>
            throw new RuntimeException(s"scp value set were inconsistent, found ($acc,$n)")
        }
      }
      val blockValue = newValue.asInstanceOf[BlockValue]
      val newBlock = blockValue.block.copy(
        hash = Hash(crypto.hash(calculateUnsignedBlockBytes(blockValue.block))))
      Some(BlockValue(newBlock))
    }
  }

  override def extractValidValue(nodeId: NodeID,
                                 slotIndex: SlotIndex,
                                 value: Value): Option[Value] = value match {
    case BlockValue(block) =>
      val newBlock = block.copy(transactions = block.transactions.filter { transaction =>
        val unsignedBytes = calculateUnsignedTransactionBytes(transaction)
        crypto.verifySignature(
          transaction.signature.value,
          unsignedBytes,
          crypto.rebuildECPublicKey(transaction.publicKeyForVerifying.value, crypto.SECP256K1))
      })
      val newHashedBlock =
        newBlock.copy(hash = Hash(crypto.hash(calculateUnsignedBlockBytes(newBlock))))
      Some(BlockValue(newHashedBlock))
  }

  override def scpExecutorService(): ExecutorService =
    Executors.newSingleThreadExecutor(
      (r: Runnable) => {
        val thread = new Thread(r)
        thread.setDaemon(true)
        thread.setName("scp-delay-executor-program-thread")
        thread
      }
    )

  override def valueConfirmed(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit = {
    value match {
      case BlockValue(block) =>
        log.info(s"confirmed block value slotIndex: ${slotIndex.value} --> hash: ${block.hash}")
      // TODO: handle value confirmed
    }
  }

  override def valueExternalized(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit = {
    value match {
      case BlockValue(block) =>
        if (block.height != slotIndex.value)
          throw new RuntimeException(
            s"can not accept block of inconsistent height, slotIndex= ${slotIndex.value} , height: ${block.height}")
        else {
          log.debug(s"to externalize: ${slotIndex.value} --> ${block.hash}")
          runner
            .runIO(CoreNodeProgram.instance.newBlockGenerated(block), coreNodeSetting)
            .unsafeRunSync()
          log.info(s"externalized: ${slotIndex.value} --> ${block.hash}")
        }
    }
  }

  override def broadcastEnvelope[M <: Message](nodeId: NodeID,
                                               slotIndex: SlotIndex,
                                               envelope: Envelope[M]): Unit = {
    val scpEnvelope      = SCPEnvelope(envelope.to[Message])
    val consensusMessage = ConsensusMessage(scpEnvelope.value.asBytesValue.bytes)
    NetworkHandler.instance.broadcastMessage(consensusMessage)(coreNodeSetting).unsafeRunSync()
  }
}
