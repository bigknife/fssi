package fssi.interpreter.scp
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.scp.interpreter.Setting
import fssi.scp.types.{NodeID, SlotIndex}
import fssi.utils._

trait SCPSupport {

  def resolveSCPSetting(coreNode: CoreNodeSetting): Setting = {
    val config               = coreNode.config.consensusConfig
    val (account, secretKey) = config.account
    val ensuredBytes         = crypto.ensure24Bytes(secretKey.value)
    val privKeyBytes =
      crypto.des3cbcDecrypt(account.encPrivKey.value, ensuredBytes, account.iv.value)
    Setting(
      initFakeValue = FakeValue(SlotIndex(0)),
      localNode = NodeID(config.account._1.pubKey.value),
      maxTimeoutSeconds = config.maxTimeoutSeconds.toInt,
      quorumSet = config.quorumSet,
      privateKey = crypto.rebuildECPrivateKey(privKeyBytes, crypto.SECP256K1),
      applicationCallback = new SCPApplicationCallback {
        override def coreNodeSetting: CoreNodeSetting = coreNode
      },
      broadcastTimeout = config.broadcastTimeout
    )
  }
}
