package fssi.interpreter.scp
import fssi.ast.uc.CoreNodeProgram
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.interpreter.{LogSupport, UnsignedBytesSupport}
import fssi.scp.interpreter.ApplicationCallback
import fssi.scp.types._
import fssi.scp.types.implicits._
import fssi.types.base.Hash
import fssi.types.implicits._
import fssi.utils._
import fssi.interpreter._
import fssi.scp.Portal

trait SCPApplicationCallback
    extends ApplicationCallback
    with UnsignedBytesSupport
    with LogSupport
    with SCPSupport {

  def coreNodeSetting: CoreNodeSetting

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
      case FakeValue(_) => Value.Validity.Invalid
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
          case _ => ac
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
    case FakeValue(_) => None
  }

  override def valueConfirmed(slotIndex: SlotIndex, value: Value): Unit = {
    value match {
      case BlockValue(block) =>
        log.info(s"confirmed block value slotIndex: ${slotIndex.value} --> hash: ${block.hash}")
      // TODO: handle value confirmed
      case FakeValue(_) =>
    }
  }

  override def valueExternalized(slotIndex: SlotIndex, value: Value): Unit = {
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
          log.info(s"start to nominate fake value on slotIndex: ${slotIndex.value + 1}")
          implicit val scpSetting: fssi.scp.interpreter.Setting = resolveSCPSetting(coreNodeSetting)
          val newSlotIndex                                      = SlotIndex(slotIndex.value + 1)
          Portal.nominateFakeValue(scpSetting.localNode, newSlotIndex, FakeValue(newSlotIndex))
          log.info(s"stop broadcast message on slotIndex: $slotIndex")
          Portal.stopBroadcastMessage()
          log.info(s"start broadcast message on slotIndex: $newSlotIndex")
          Portal.startBroadcastMessage(newSlotIndex)
        }
      case FakeValue(_) =>
    }
  }

  override def broadcastEnvelope[M <: Message](slotIndex: SlotIndex,
                                               envelope: Envelope[M]): Unit = {
    import io.circe._
    import io.circe.syntax._
    import io.circe.generic.auto._
    import fssi.scp.interpreter.json.implicits._
    import fssi.interpreter.scp.BlockValue.implicits._
    val transferredEnvelope = envelope.to[Message]
    val scpEnvelope         = SCPEnvelope(transferredEnvelope)
    if (slotIndex.value == envelope.statement.slotIndex.value) {
      log.debug(s"broadcast scp envelope ${transferredEnvelope.asJson.noSpaces}")
      runner
        .runIOAttempt(CoreNodeProgram.instance.broadcastConsensusMessage(scpEnvelope),
                      coreNodeSetting)
        .unsafeRunSync() match {
        case Right(_) =>
          log.info(s"broadcast scp envelope ${transferredEnvelope.asJson.noSpaces} success")
        case Left(e) => log.error(s"broadcast scp envelope failed: ${e.getMessage}", Some(e))
      }
    } else
      throw new RuntimeException(
        s"can't broadcast envelope of inconsistent slot index, received: ${slotIndex.value} , envelope: ${envelope.statement.slotIndex.value}")
  }
}
