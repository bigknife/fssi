package fssi
package sandbox
package inf

import java.io.File

import com.typesafe.config._
import fssi.sandbox.types.ContractMeta.{MethodDescriptor, Name, Owner, Version}

import scala.collection.JavaConverters._
import fssi.sandbox.types.Protocol._

case class ConfigReader(configFile: File) {

  lazy val config: Config = ConfigFactory.parseFile(configFile)

  lazy val owner: Owner = Owner(config.getString(ownerKey))

  lazy val name: Name = Name(config.getString(nameKey))

  lazy val version: Version = Version(config.getString(versionKey))

  lazy val methodDescriptors: Vector[MethodDescriptor] = config
    .getObject(interfacesKey)
    .entrySet
    .asScala
    .toVector
    .foldLeft(Vector.empty[MethodDescriptor]) { (acc, n) =>
      val alias      = n.getKey
      val descriptor = n.getValue.unwrapped.toString
      acc :+ MethodDescriptor(alias, descriptor)
    }

  def hasConfigKey(keyName: String): Boolean = {
    config.hasPath(keyName)
  }
}
