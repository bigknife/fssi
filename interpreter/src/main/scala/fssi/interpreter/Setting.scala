package fssi.interpreter

import java.nio.file.Paths

/** interpreter's setting */
case class Setting(
    workingDir: String = Paths.get(System.getProperty("user.home"), ".fssi").toString,
    snapshotDbPort: Int = 18080,
    startSnapshotDbConsole: Boolean = false,
    snapshotDbConsolePort: Int = 18081
) {
  val snapshotDbBaseDir: String = Paths.get(workingDir, "snapshotdb").toString
  val nodeJsonFile: String = Paths.get(workingDir, ".node").toString
}
