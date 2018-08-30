package fssi
package interpreter
package scp

import bigknife.scalap.world.Connect
import bigknife.scalap.ast.types._
import bigknife.scalap.interpreter.{runner => scprunner}
import bigknife.scalap.ast.usecase.{SCP, component => scpcomponent}

import utils._

import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import types.json.implicits._
import types.{JsonMessage, Account, HexString}
import ast._, uc._

import jsonCodecs._
import org.slf4j._

/** To solve Recursive-Dependency problem.
  * ConsensusConnect needs CoreNodeSetting, and CoreNodeSetting needs ConsensusConnect, so they are dependent on each
  * other recursively.
  * To solve this problem, we use lazy-init to delay the dependency-resolving.
  *
  * @param getSetting lazily to get CoreNodeSetting.
  */
case class ConsensusConnect(getSetting: () => Setting.CoreNodeSetting)
    extends Connect
    with BlockCalSupport
    with SCPSupport
    with LogSupport {

  lazy val setting: Setting.CoreNodeSetting = getSetting()

  val scp: SCP[scpcomponent.Model.Op]                       = SCP[scpcomponent.Model.Op]
  val coreNodeProgram: CoreNodeProgram[components.Model.Op] = CoreNodeProgram[components.Model.Op]

  private val nominatingCounters: SafeVar[Map[SlotIndex, Int]] = SafeVar(Map.empty)

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

          //todo: we should verify this transaction can be run, and check
          //      such as:
          //              1. for a transfer,  we should ensure the account has enough token
          //              2. for a publish contract, maybe some checks are mandatory
          //              3. ...

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

    val privateKey = crypto.des3cbcDecrypt(boundAccount.encryptedPrivateKey.bytes,
                                           crypto.ensure24Bytes(BytesValue(setting.password)).bytes,
                                           boundAccount.iv.bytes)

    Signature(
      crypto.makeSignature(
        source = bytes,
        priv = crypto.rebuildECPrivateKey(privateKey)
      ))
  }

  /**
    * broadcast message
    * @param envelope envelope
    */
  def broadcastMessage[M <: Message](envelope: Envelope[M]): Unit = {
    // encode envelope to JsonMessage
    // then let network send it
    val jsonMessage = JsonMessage.scpEnvelopeJsonMessage(envelope.asJson.noSpaces)
    NetworkHandler.instance.broadcastMessage(jsonMessage)(setting).unsafeRunSync
  }

  /**
    * broad node quorum set
    * @param nodeID node
    * @param quorumSet quorum set
    */
  def synchronizeQuorumSet(nodeID: NodeID, quorumSet: QuorumSet): Unit = {
    // encode (nodeID -> quorumSet) to JsonMessage
    // then let network send it
    // todo: we need renew qs sync protocol
    //       now, take it easy, only broadcast local node and it's qs
    //       other nodes should merge every qss message, then they will
    //       get all nodes' qs finally.
    val version     = 0L
    val qs          = Map(nodeID -> quorumSet)
    val hash        = QuorumSetSync.hash(version, qs)
    val qss         = QuorumSetSync(version, qs, hash)
    val jsonMessage = JsonMessage.scpQsSyncJsonMessage(qss.asJson.noSpaces)

    new java.util.Timer().scheduleAtFixedRate(new java.util.TimerTask {
      override def run(): Unit = {
        NetworkHandler.instance.broadcastMessage(jsonMessage)(setting).unsafeRunSync
        log.debug("broadcast scp quorum set sync message")
      }

    }, 5000, 5000)

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
          if (accBlock.block.chainID != nBlock.block.chainID)
            throw new RuntimeException(s"scp value set were not consistent," +
              s" found chainId = ${accBlock.block.chainID}, ${nBlock.block.chainID} at the same time")
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
  def runAbandonBallot(nodeID: NodeID, slotIndex: SlotIndex, counter: Int): Unit = {
    val p = scp.abandonBallot(nodeID, slotIndex, counter)
    scprunner.runIO(p, unsafeResolveSCPSetting(nodeID, setting)).unsafeRunSync
  }

  /**
    * triggered when value externalized
    * @param nodeID node id
    * @param slotIndex slotIndex
    * @param value value
    */
  def valueExternalized(nodeID: NodeID, slotIndex: SlotIndex, value: Value): Unit = {

    // stop nominating for slotIndex
    nominatingCounters.updated {x =>
      x + (slotIndex -> -1)
    }
    log.debug(s"set nominating counter to -1 for slot($slotIndex)")

    value match {
      case BlockValue(block, _ /*bytes*/ ) =>
        if (block.height != slotIndex.index)
          throw new RuntimeException(
            s"can't accept block of inconsistent height, slotIndex=${slotIndex.index}, " +
              s"but block height is ${block.height}")
        else {
          // persist block into local store.
          // the logic is defined in CoreNodeProgram
          log.debug(s"to externalize: ${slotIndex.index} -> ${block.hash}")
          runner
            .runIO(coreNodeProgram.handleBlockReachedAgreement(Account.ID(HexString(nodeID.bytes)),
                                                               slotIndex.index,
                                                               block),
                   setting)
            .unsafeRunSync
          log.info(s"externalized: ${slotIndex.index} -> ${block.hash}")
        }
      case _ => throw new RuntimeException(s"can't accept value: $value")
    }
  }

  /**
    * timeout for next round
    * @param currentRound current round
    * @return timeout milliseconds
    */
  def timeoutForNextRoundNominating(currentRound: Int): Long = {
    // todo: maybe a linear variation
    // the first ten rounds run every 1seconds, and then linear increasing
    if (currentRound > 10) (currentRound - 10) * 1000L
    else 1000L
  }

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
                                 afterMilliSeconds: Long): Unit = {
    // if current nominating process is stopped(because of reaching agreement), don't trigger next round
    // we use the counter == -1 to represent this situation
    // and it new slotIndex has come , remove other counters.

    // if the counter of the slotIndex is over threshold, DO NOT start a timer,
    // means, don't nominate any more
    val maxNominatingTimes     = setting.configReader.coreNode.scp.maxNominatingTimes
    val currentNominatingTimes = nominatingCounters.map(_.getOrElse(slotIndex, 0)).unsafe()
    if (currentNominatingTimes > setting.configReader.coreNode.scp.maxNominatingTimes) {
      log.info(s"current nominating times($currentNominatingTimes) is over($maxNominatingTimes)")
    }
    else if (currentNominatingTimes == -1) {
      // means current slotIndex has reached to agreement.
      log.info(s"current slot ($slotIndex) reached to agreement")
    }
    else {
      nominatingCounters.updated { map =>
        map + (slotIndex -> (currentNominatingTimes + 1))
      }
      val timer = new java.util.Timer()
      timer.schedule(
        new java.util.TimerTask {
          override def run(): Unit = {
            log.info("run nominate timer ...")
            // only re-nominate when slotIndex is currentHeight + 1
            val currentHeight =
              runner.runIO(coreNodeProgram.currentHeight(), setting).unsafeRunSync()
            if (slotIndex.index == (currentHeight + 1)) {
              SCPExecutionService.submit {
                val p = scp.nominate(nodeID, slotIndex, nextRound, valueToNominate, previousValue)
                val ret =
                  scprunner.runIO(p, unsafeResolveSCPSetting(nodeID, setting)).unsafeRunSync()
                log.info(s"nominate result: $ret")
              }
              log.info("end and cancel nominate timer ...")
              timer.cancel()
            } else {
              log.info(s"slotIndex[${slotIndex.index}] has been determined, timer cancel!")
              timer.cancel()
            }
          }
        },
        afterMilliSeconds
      )
    }
  }
}

object ConsensusConnect {
  val DuckConnect: Connect = new Connect {
    override def extractValidValue(value: Value): Option[Value]                              = ???
    override def validateValue(value: Value): Value.Validity                                 = ???
    override def signData(bytes: Array[Byte], nodeID: NodeID): Signature                     = ???
    override def broadcastMessage[M <: Message](envelope: Envelope[M]): Unit                 = ???
    override def synchronizeQuorumSet(nodeID: NodeID, quorumSet: QuorumSet): Unit            = ???
    override def verifySignature[M <: Message](envelope: Envelope[M]): Boolean               = ???
    override def combineValues(valueSet: ValueSet): Value                                    = ???
    override def runAbandonBallot(nodeID: NodeID, slotIndex: SlotIndex, counter: Int): Unit  = ???
    override def valueExternalized(nodeID: NodeID, slotIndex: SlotIndex, value: Value): Unit = ???
    override def timeoutForNextRoundNominating(currentRound: Int): Long                      = ???
    override def triggerNextRoundNominating(nodeID: NodeID,
                                            slotIndex: SlotIndex,
                                            nextRound: Int,
                                            valueToNominate: Value,
                                            previousValue: Value,
                                            afterMilliSeconds: Long): Unit = ???
  }
}
