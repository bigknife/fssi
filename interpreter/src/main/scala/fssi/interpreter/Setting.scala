package fssi.interpreter

import java.nio.file.Paths

import bigknife.scalap.ast.types._
import fssi.ast.domain.Node
import bigknife.scalap.interpreter.{Setting => ScalapSetting}
import bigknife.scalap.world.Connect

/** interpreter's setting */
case class Setting(
    workingDir: String = Paths.get(System.getProperty("user.home"), ".fssi").toString,
    snapshotDbPort: Int = 18080,
    startSnapshotDbConsole: Boolean = false,
    snapshotDbConsolePort: Int = 18081,
    warriorNodesOfNymph: Vector[Node.Address] = Vector.empty,
    maxMomentSize: Int = 20,
    maxMomentPoolElapsedSecond: Int = 3,
    scpRegisteredQuorumSets: Map[NodeID, QuorumSet] = Map.empty,
    scpConnect: Connect = Connect.dummy,
    scpMaxTimeoutSeconds: Int = 30 * 60
) {
  // try create working dir
  new java.io.File(workingDir).mkdirs()

  val snapshotDbBaseDir: String = Paths.get(workingDir, "snapshotdb").toString
  val nodeJsonFile: String      = Paths.get(workingDir, ".node").toString
  val contractTempDir: String   = Paths.get(workingDir, "temp/contract").toString

  def toScalapSetting(nodeID: NodeID): ScalapSetting = ScalapSetting(
    nodeID,
    scpRegisteredQuorumSets(nodeID),
    scpConnect,
    scpMaxTimeoutSeconds,
    scpRegisteredQuorumSets
  )
}
