package fssi.interpreter.scp
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.scp.interpreter.Setting
import fssi.scp.types.NodeID
import fssi.utils._

trait SCPSupport {

  def resolveSCPSetting(coreNode: CoreNodeSetting): Setting = {
    val config = coreNode.config.consensusConfig
    Setting(
      localNode = NodeID(config.account.pubKey.value),
      maxTimeoutSeconds = config.maxTimeoutSeconds.toInt,
      quorumSet = config.quorumSet,
      privateKey = crypto.rebuildECPrivateKey(config.account.encPrivKey.value, crypto.SECP256K1),
      applicationCallback = new SCPApplicationCallback {
        override def coreNodeSetting: CoreNodeSetting = coreNode
      }
    )
  }
}
