package fssi
package sandbox
package types
import fssi.sandbox.counter.CostCounter
import org.objectweb.asm.Type

object Protocol {

  lazy val majVersion     = 1
  lazy val version        = s"$majVersion.0.0"
  lazy val majJavaVersion = 8

  lazy val contractSize: Long = 1024 * 200

  lazy val forbiddenDescriptor = Vector(
    "^Ljava/util/concurrent.*",
    "^Ljava/lang/reflect.*",
    "^Ljava/lang/Thread;",
    "^Ljava/lang/Class;",
    "^Ljavax/.*",
    "^Lsun/.*",
    "^Ljava/net.*",
    "^Ljava/sql.*"
  )

  lazy val ignoreDescriptors = Vector(
    "^Ljava/lang/Object;",
    "^Ljava/lang/String;",
    "^Lfssi/.*",
    "^\\[*C$",
    "^\\[*Z$",
    "^\\[*B$",
    "^\\[*S$",
    "^\\[*I$",
    "^\\[*F$",
    "^\\[*J$",
    "^\\[*D$"
  )

  lazy val contractFileName = "contract"
  lazy val versionFileName  = "version"

  lazy val allowedResourceFiles = Vector(contractFileName, versionFileName)

  lazy val forbiddenPackage = Vector("fssi")

  lazy val counterInternalName: String = Type.getInternalName(classOf[CostCounter])
}
