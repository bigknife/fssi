package fssi.interpreter

import java.nio.file.Paths

/** interpreter's setting */
case class Setting(
    snapshotDbBaseDir: String =Paths.get(System.getProperty("user.home"), ".fssi").toString,
    snapshotDbPort: Int = 18080,
    startSnapshotDbConsole: Boolean = false,
    snapshotDbConsolePort: Int = 18081
)
