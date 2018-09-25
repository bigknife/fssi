package fssi
package sandbox
package config

import java.io.File
import com.typesafe.config._
import fssi.sandbox.types._
import scala.collection.JavaConverters._

case class ConfigReader(configFile: File) {

  lazy val config: Config = ConfigFactory.parseFile(configFile)

  lazy val owner: Owner = Owner(config.getString("contract.owner"))

  lazy val name: Name = Name(config.getString("contract.name"))

  lazy val version: Version = Version(config.getString("contract.version"))

  lazy val interfaces: Vector[MethodDescriptor] = config
    .getObject("contract.interfaces")
    .entrySet
    .asScala
    .toVector
    .foldLeft(Vector.empty[MethodDescriptor]) { (acc, n) =>
      val alias      = n.getKey
      val descriptor = n.getValue.unwrapped.toString
      acc :+ MethodDescriptor(alias, descriptor)
    }

}
