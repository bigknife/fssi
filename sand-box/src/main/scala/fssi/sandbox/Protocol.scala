package fssi
package sandbox

object Protocol {

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

  lazy val allowedResourceFiles = Vector("contract", "version")

  lazy val forbiddenPackage = Vector("fssi")
}
