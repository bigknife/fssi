package fssi
package interpreter
package scp

import bigknife.scalap.world.Connect
import bigknife.scalap.ast.types._

import utils._

import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import types.json.implicits._
import types.JsonMessage

import jsonCodecs._

trait ConsensusConnect extends Connect with HandlerCommons {
  val setting: Setting
  /**
    * try to extract a valid value from a not full validated value
    * @param value value not full validated
    * @return if possible, Some extracted value, or None
    */
  def extractValidValue(value: Value): Option[Value] = value match {
    case BlockValue(block, _) =>
      // filter the badly signed transaction
      val newBlock = block.copy(
        transactions = block.transactions.filter { transaction =>
          val signedBytes = calculateBytesToBeSignedOfTransaction(transaction)
          crypto.verifySignature(
            sign = transaction.signature.value.bytes,
            source = signedBytes.value,
            publ = crypto.rebuildECPublicKey(transaction.sender.value.bytes)
          )
        }
      )
      Some(BlockValue(hashBlock(newBlock)))
    case _ => None
  }

  /**
    * application level validation
    * @param value value
    * @return full/ maybe/ invalid
    */
  def validateValue(value: Value): Value.Validity = {
    // value should be BlockValue, or it's invalid
    value match {
      case BlockValue(block, _ /*bytes*/ ) =>
        // hash should be consistent
        val h = hashBlock(block)
        if (h.hash == block.hash) {
          // to see every transaction if they have signed correctly
          val exitBadSignature = block.transactions.exists { transaction =>
            val signedBytes = calculateBytesToBeSignedOfTransaction(transaction)
            !crypto.verifySignature(
              sign = transaction.signature.value.bytes,
              source = signedBytes.value,
              publ = crypto.rebuildECPublicKey(transaction.sender.value.bytes)
            )
          }

          val exitGoodSignature = block.transactions.exists { transaction =>
            val signedBytes = calculateBytesToBeSignedOfTransaction(transaction)
            crypto.verifySignature(
              sign = transaction.signature.value.bytes,
              source = signedBytes.value,
              publ = crypto.rebuildECPublicKey(transaction.sender.value.bytes)
            )
          }

          (exitBadSignature, exitGoodSignature) match {
            case (true, true)  => Value.Validity.MaybeValid
            case (false, true) => Value.Validity.FullyValidated
            case _             => Value.Validity.Invalid
          }

        } else Value.Validity.Invalid

      case _ => Value.Validity.Invalid
    }
  }

  /**
    * make a signature for data of a node
    * @param bytes data
    * @param nodeID node id
    * @return
    */
  def signData(bytes: Array[Byte], nodeID: NodeID): Signature = {
    // note: this is a hack op, we dived into NetworkHandler, then found
    // that getCurrentnode function ignored the setting argument
    // so, we can provide a DefaultSetting to cheat the invocation.
    val node = NetworkHandler.instance.getCurrentNode()(Setting.DefaultSetting).unsafeRunSync

    // and it's determined that node's bound account is not empty.
    val boundAccount = node.account.get

    Signature(
      crypto.makeSignature(
        source = bytes,
        priv = crypto.rebuildECPrivateKey(boundAccount.encryptedPrivateKey.bytes)
      ))
  }

  /**
    * broadcast message
    * @param envelope envelope
    */
  def broadcastMessage[M <: Message](envelope: Envelope[M]): Unit = {
    // encode envelope to JsonMessage
    // then let network send it
    val jsonMessage = JsonMessage.scpJsonMessage(envelope.asJson.noSpaces)

    // note: this is a hack op, we dived into NetworkHandler, then found
    // that broadcastMessage function ignored the setting argument
    // so, we can provide a DefaultSetting to cheat the invocation.
    NetworkHandler.instance.broadcastMessage(jsonMessage)(Setting.DefaultSetting).unsafeRunSync
  }

  /**
    * broad node quorum set
    * @param nodeID node
    * @param quorumSet quorum set
    */
  def synchronizeQuorumSet(nodeID: NodeID, quorumSet: QuorumSet): Unit = {

  }

  /**
    * verify signature of an envelope
    * @param envelope envelope
    * @return
    */
  def verifySignature[M <: Message](envelope: Envelope[M]): Boolean = {
    import bigknife.scalap.ast.types.implicits._
    val source: Array[Byte] = envelope.statement match {
      case x: Statement.Nominate    => x.bytes
      case x: Statement.Prepare     => x.bytes
      case x: Statement.Commit      => x.bytes
      case x: Statement.Externalize => x.bytes
    }
    crypto.verifySignature(
      sign = envelope.signature.bytes,
      source = source,
      publ = crypto.rebuildECPublicKey(envelope.statement.nodeID.bytes)
    )
  }

  /**
    * combine value set to one value
    * @param valueSet values
    * @return
    */
  def combineValues(valueSet: ValueSet): Value = {
    if (valueSet.isEmpty) Value.bottom
    else {
      val values: Vector[Value] = valueSet.toVector
      val newValue = values.foldLeft(Value.bottom) { (acc, n) =>
        if (acc.isEmpty) n
        else {
          val accBlock = acc.asInstanceOf[BlockValue]
          val nBlock   = n.asInstanceOf[BlockValue]

          // if found block height not consistent, throw exception
          if (accBlock.block.height != nBlock.block.height)
            throw new RuntimeException(s"scp value set were not consistent," +
              s" found height = ${accBlock.block.height}, ${nBlock.block.height} at the same time")
          else ()

          // if found chainID not consistent, throw exception
          if (accBlock.block.chainID != nBlock.block.height)
            throw new RuntimeException(s"scp value set were not consistent," +
              s" found chainId = ${accBlock.block.height}, ${nBlock.block.height} at the same time")
          else ()

          // combine transactions
          accBlock.copy(
            block = accBlock.block.copy(
              transactions = accBlock.block.transactions ++ nBlock.block.transactions))
        }
      }
      val newBlockValue: BlockValue = newValue.asInstanceOf[BlockValue]
      val newBlock                  = hashBlock(newBlockValue.block)
      BlockValue(newBlock, calculateTotalBlockBytes(newBlock))
    }
  }

  /**
    * run abandon ballot with counter outside
    * @param counter ballot's counter
    */
  def runAbandonBallot(nodeID: NodeID, slotIndex: SlotIndex, counter: Int): Unit = ???

  /**
    * triggered when value externalized
    * @param nodeID node id
    * @param slotIndex slotIndex
    * @param value value
    */
  def valueExternalized(nodeID: NodeID, slotIndex: SlotIndex, value: Value): Unit = ???

  /**
    * timeout for next round
    * @param currentRound current round
    * @return timeout milliseconds
    */
  def timeoutForNextRoundNominating(currentRound: Int): Long = ???

  /**
    * trigger next round nominating
    * @param nodeID node id
    * @param slotIndex slotIndex
    * @param nextRound next round number
    * @param valueToNominate value to nominate
    * @param previousValue previous value
    * @param afterMilliSeconds after millis seconds
    * @return
    */
  def triggerNextRoundNominating(nodeID: NodeID,
                                 slotIndex: SlotIndex,
                                 nextRound: Int,
                                 valueToNominate: Value,
                                 previousValue: Value,
                                 afterMilliSeconds: Long): Unit = ???
}
