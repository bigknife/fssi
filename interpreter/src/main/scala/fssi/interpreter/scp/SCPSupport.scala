package fssi.interpreter.scp
import fssi.interpreter.Configuration.ConsensusConfig
import fssi.scp.interpreter.Setting
import fssi.scp.types.NodeID
import fssi.utils._

trait SCPSupport {

  def resolveSCPSetting(config: ConsensusConfig): Setting = {
    Setting(
      nodeId = NodeID(config.account.pubKey.value),
      maxTimeoutSeconds = config.maxTimeoutSeconds.toInt,
      quorumSet = config.quorumSet,
      privateKey = crypto.rebuildECPrivateKey(config.account.encPrivKey.value, crypto.SECP256K1),
      applicationCallback = new SCPApplicationCallback
    )
  }
}
