package fssi.interpreter.scp
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.scp.interpreter.Setting
import fssi.scp.types.NodeID
import fssi.utils._

trait SCPSupport {

  def resolveSCPSetting(coreNode: CoreNodeSetting): Setting = {
    val config               = coreNode.config.consensusConfig
    val (account, secretKey) = config.account
    val ensuredBytes         = crypto.ensure24Bytes(secretKey.value)
    val privKeyBytes =
      crypto.des3cbcDecrypt(account.encPrivKey.value, ensuredBytes, account.iv.value)
    Setting(
      localNode = NodeID(config.account._1.pubKey.value),
      maxTimeoutSeconds = config.maxTimeoutSeconds.toInt,
      quorumSet = config.quorumSet,
      privateKey = crypto.rebuildECPrivateKey(privKeyBytes, crypto.SECP256K1),
      applicationCallback = SCPApplicationCallback(coreNode),
      broadcastTimeout = config.broadcastTimeout
    )
  }
}
