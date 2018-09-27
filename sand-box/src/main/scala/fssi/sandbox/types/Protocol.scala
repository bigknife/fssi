package fssi
package sandbox
package types
import fssi.sandbox.counter.CostCounter
import org.objectweb.asm.Type

object Protocol {

  lazy val magic = "FSSI"

  lazy val majVersion     = 1
  lazy val version        = s"$majVersion.0.0"
  lazy val majJavaVersion = 8

  lazy val contractSize: Long = 1024 * 200

  lazy val forbiddenDescriptor = Vector(
    "^Ljava/util/concurrent.*",
    "^Ljava/lang/reflect.*",
    "^Ljava/net.*",
    "^Ljava/sql.*",
    "^Ljavax/.*",
    "^Lsun/.*",
    "^Ljava/lang/Thread;",
    "^Ljava/lang/Class;"
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

  lazy val metaFileName = "contract.meta.conf"

  lazy val ownerKey      = "contract.owner"
  lazy val nameKey       = "contract.name"
  lazy val versionKey    = "contract.version"
  lazy val interfacesKey = "contract.interfaces"

  lazy val allowedResourceFiles = Vector(metaFileName)

  lazy val forbiddenPackage = Vector("fssi")

  lazy val counterInternalName: String = Type.getInternalName(classOf[CostCounter])
}
