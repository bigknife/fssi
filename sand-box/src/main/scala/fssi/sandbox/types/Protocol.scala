package fssi
package sandbox
package types
import fssi.sandbox.counter.CostCounter
import org.objectweb.asm.Type

object Protocol {

  lazy val contractSize: Long = 1024 * 200

  lazy val forbiddenClasses = Vector(
    "^Ljava/util/concurrent.*",
    "^Ljava/lang/reflect.*",
    "^Ljava/lang/Thread;",
    "^Ljava/lang/Class;",
    "^Ljavax/.*",
    "^Lsun/.*",
    "^Ljava/net.*",
    "^Ljava/sql.*"
  )

  lazy val allowedClasses = Vector(
    "^Ljava/lang/Object;"
  )

  lazy val ignoreClasses = Vector(
    "^Lfssi/.*"
  )

  lazy val contractFileName = "contract"
  lazy val versionFileName  = "version"

  lazy val allowedResourceFiles = Vector(contractFileName, versionFileName)

  lazy val forbiddenPackage = Vector("fssi")

  lazy val counterInternalName: String = Type.getInternalName(classOf[CostCounter])
}
