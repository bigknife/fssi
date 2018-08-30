package fssi
package interpreter

import utils._
import types._, implicits._
import ast._

import scala.collection._

import bigknife.scalap.ast.types.{NodeID, SlotIndex}
import bigknife.scalap.interpreter.{Setting => SCPSetting}

/** supports for integerating SCP
  */
trait SCPSupport {

  /** try to resovle SCPSetting from a setting object
    */
  private[interpreter] def resolveSCPSetting(localNodeID: NodeID,
                                             setting: Setting): Option[SCPSetting] = {
    require(setting != null, "SCPSupport should not be null!!!")
    setting match {
      case x: Setting.CoreNodeSetting =>
        val qs = x.configReader.coreNode.scp.quorumSet
        val scpSetting = SCPSetting(
          localNodeID = localNodeID,
          quorumSet = qs,
          connect = x.consensusConnect,
          maxTimeoutSeconds = x.configReader.coreNode.scp.maxTimeoutSeconds,
          presetQuorumSets = scala.collection.immutable.Map(localNodeID -> qs)
        )
        Some(scpSetting)

      case _ => None // only core node config can be translated into scpsetting
    }
  }

  /** unsafe operation for resovle SCPSetting from a setting object
    */
  private[interpreter] def unsafeResolveSCPSetting(account: Account, setting: Setting): SCPSetting =
    resolveSCPSetting(NodeID(account.id.value.bytes), setting).get

  /** unsafe operation for resovle SCPSetting from a setting object
    */
  private[interpreter] def unsafeResolveSCPSetting(localNodeID: NodeID,
                                                   setting: Setting): SCPSetting = {
    val resolved = resolveSCPSetting(localNodeID, setting)
    resolved.get
  }

}
