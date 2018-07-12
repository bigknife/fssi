package fssi.interpreter

import java.nio.file.Paths

import bigknife.scalap.ast.types._
import fssi.ast.domain.Node
import bigknife.scalap.interpreter.{Setting => ScalapSetting}
import bigknife.scalap.world.Connect
import fssi.ast.domain.types.{Account, BytesValue}
import io.circe.{Json, JsonObject}

/** interpreter's setting */
case class Setting(
    workingDir: String = Paths.get(System.getProperty("user.home"), ".fssi").toString,
    snapshotDbPort: Int = 18080,
    startSnapshotDbConsole: Boolean = false,
    snapshotDbConsolePort: Int = 18081,
    warriorNodesOfNymph: Vector[Node.Address] = Vector.empty,
    maxMomentSize: Int = 20,
    maxMomentPoolElapsedSecond: Int = 3,
    scpConnect: Connect = Connect.dummy,
    scpMaxTimeoutSeconds: Int = 30 * 60,
    boundAccount: Option[Account] = None
) {
  // try create working dir
  new java.io.File(workingDir).mkdirs()

  val snapshotDbBaseDir: String = Paths.get(workingDir, "snapshotdb").toString
  val nodeJsonFile: String      = Paths.get(workingDir, ".node").toString
  val contractTempDir: String   = Paths.get(workingDir, "temp/contract").toString

  lazy val scpRegisteredQuorumSets: Map[NodeID, QuorumSet] = {
    // read from scp conf file (json)
    // { registeredQuorumSets: [{"nodeId": "nodeId", quorumSet : {quorumset...}}]}
    import io.circe.parser._
    import io.circe.syntax._
    import jsonCodec._
    val scpConfFile = better.files.File(Paths.get(workingDir, ".scp.json"))
    parse(scpConfFile.contentAsString) match {
      case Left(t) => throw new RuntimeException("scp conf parse failed", t)
      case Right(json) =>
        try {
          val qs: Vector[JsonObject] =
            json.asObject.get("registeredQuorumSets").get.asArray.get.map(_.asObject).map(_.get)

          qs.map { jso =>
            val accountPublicKeyHex = jso("nodeID").get.as[String].right.get
            val q = jso("quorumSet").get.as[QuorumSet].right.get
            val nodeID = NodeID(BytesValue.decodeHex(accountPublicKeyHex).bytes)
            nodeID -> q
          }.toMap

        } catch {
          case x: Throwable => throw new RuntimeException("scp conf parse failed", x)
        }

    }

  }

  def toScalapSetting(nodeID: NodeID): ScalapSetting = ScalapSetting(
    nodeID,
    scpRegisteredQuorumSets(nodeID),
    scpConnect,
    scpMaxTimeoutSeconds,
    scpRegisteredQuorumSets
  )
}
