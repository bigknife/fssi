package fssi.interpreter

import java.nio.file.Paths

import fssi.ast.domain.Node

/** interpreter's setting */
case class Setting(
    workingDir: String = Paths.get(System.getProperty("user.home"), ".fssi").toString,
    snapshotDbPort: Int = 18080,
    startSnapshotDbConsole: Boolean = false,
    snapshotDbConsolePort: Int = 18081,
    warriorNodesOfNymph: Vector[Node.Address] = Vector.empty
) {
  // try create working dir
  new java.io.File(workingDir).mkdirs()

  val snapshotDbBaseDir: String = Paths.get(workingDir, "snapshotdb").toString
  val nodeJsonFile: String = Paths.get(workingDir, ".node").toString
  val contractTempDir: String = Paths.get(workingDir, "temp/contract").toString
}
