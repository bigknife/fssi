package fssi.world

import java.io.File
import java.nio.file.Paths

import fssi.ast.domain.Node.Address
import fssi.ast.domain.types.{Account, BytesValue, Token}
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
      boundAccountPublicKey: String = "",
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
      publicKey: String = "",
      privateKey: String = "",
      iv: String = "",
      pass: Array[Byte] = Array.emptyByteArray,
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
      snapshotDbConsolePort = snapshotDbConsolePort,
      boundAccount = Some(
        Account(Account.emptyID,
                BytesValue(boundAccountPrivateKey),
                BytesValue(boundAccountPublicKey),
                BytesValue.decodeHex(iv),
                Token.Zero)
      )
    )

    lazy val boundAccountPublicKey: Array[Byte] = BytesValue.decodeHex(publicKey).bytes
    lazy val boundAccountPrivateKey: Array[Byte] = {
      import fssi.interpreter.util.crypto
      crypto
        .des3cbcDecrypt(BytesValue.decodeHex(privateKey),
                        crypto.enforceDes3Key(BytesValue(pass)),
                        BytesValue.decodeHex(iv))
        .bytes
    }
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

  // cmd tool, run a contract with params
  case class CreateRunContractArgs(
      accountId: String = "",
      name: String = "",
      version: String = "",
      function: String = "",
      params: String = "",
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
