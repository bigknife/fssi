package fssi.world

import java.nio.file.Paths

import fssi.ast.domain.Node.Address
import fssi.interpreter.Setting

sealed trait Args {
  def toSetting: Setting
}

object Args {
  case class EmptyArgs() extends Args {
    override def toSetting: Setting = Setting()
  }

  case class NymphArgs(
      workingDir: String = Paths.get(System.getProperty("user.home"), ".fssi").toString,
      snapshotDbPort: Int = 18080,
      startSnapshotDbConsole: Boolean = false,
      snapshotDbConsolePort: Int = 18081,
      seeds: Vector[String] = Vector.empty,
      jsonrpcPort: Int = 8080,
      jsonrpcHost: String = "0.0.0.0",
      jsonrpcServiceName: String = "nymph",
      jsonrpcServiceVersion: String = "v1",
      nodePort: Int = 28080,
      nodeIp: String = "0.0.0.0",
      warriorNodes: Vector[String] = Vector.empty,
      verbose: Boolean = false,
      colorfulLog: Boolean = false
  ) extends Args {
    lazy val toSetting: Setting = Setting().copy(
      workingDir = workingDir,
      snapshotDbPort = snapshotDbPort,
      startSnapshotDbConsole = startSnapshotDbConsole,
      snapshotDbConsolePort = snapshotDbConsolePort,
      warriorNodes.map(Address.apply)
    )
  }

  case class WarriorArgs(
      workingDir: String = Paths.get(System.getProperty("user.home"), ".fssi").toString,
      snapshotDbPort: Int = 18080,
      startSnapshotDbConsole: Boolean = false,
      snapshotDbConsolePort: Int = 18081,
      seeds: Vector[String] = Vector.empty,
      nodePort: Int = 28081,
      nodeIp: String = "0.0.0.0",
      verbose: Boolean = false,
      colorfulLog: Boolean = false
  ) extends Args {
    lazy val toSetting: Setting = Setting().copy(
      workingDir = workingDir,
      snapshotDbPort = snapshotDbPort,
      startSnapshotDbConsole = startSnapshotDbConsole,
      snapshotDbConsolePort = snapshotDbConsolePort
    )
  }

  // cmd tool, create an account
  case class CreateAccountArgs(
      pass: String = "88888888"
  ) extends Args {
    override def toSetting: Setting = Setting()
  }

  // cmd tool, create a transfer transaction
  // transfer token from current account to another one.
  case class CreateTransferArgs(
      accountId: String = "",
      transferTo: String = "",
      amount: Long = 0,
      privateKey: String = "",
      password: String = "",
      iv: String = "",
      outputFormat: String = "nospace" // or space2, or space4
  ) extends Args {
    override def toSetting: Setting = Setting()
  }

  // cmd tool, create a publishContract transaction
  case class CreatePublishContractArgs(
      accountId: String = "",
      name: String = "",
      version: String = "",
      contract: String = "",
      privateKey: String = "",
      password: String = "",
      iv: String = "",
      outputFormat: String = "nospace" // or space2, or space4
  ) extends Args {
    override def toSetting: Setting = Setting()
  }

  // cmd tool, compile a contract project
  case class CompileContractArgs(
      projectDir: String = "",
      outputFormat: String = "", // jar, hex or base64
      outputFile: String = ""
  ) extends Args {
    override def toSetting: Setting = Setting()
  }

  def default: Args = EmptyArgs()

  trait Implicits  extends ArgsParser
  object implicits extends Implicits
}
