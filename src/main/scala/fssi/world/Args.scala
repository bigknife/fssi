package fssi.world

import java.nio.file.Paths

import fssi.ast.domain.Node.Address
import fssi.interpreter.Setting

sealed trait Args {
  def toSetting: Setting = Setting()
}

object Args {
  case class EmptyArgs() extends Args

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
      warriorNodes: Vector[String] = Vector.empty
  ) extends Args {
    override def toSetting: Setting = Setting().copy(
      workingDir = workingDir,
      snapshotDbPort = snapshotDbPort,
      startSnapshotDbConsole = startSnapshotDbConsole,
      snapshotDbConsolePort = snapshotDbConsolePort,
      warriorNodes.map(Address.apply)
    )
  }

  case class CreateAccountArgs(
      pass: String = "88888888"
  ) extends Args

  def default: Args = EmptyArgs()

  trait Implicits {

    implicit object parser extends _root_.scopt.OptionParser[Args]("fssi") {
      head("fssi", "0.0.1")
      help("help").abbr("h").text("print this help documents")

      cmd("nymph")
        .text("run nymph jsonrpc server")
        .action((_, _) => NymphArgs())
        .children(
          opt[String]("working-dir")
            .text("nymph working dir")
            .action((s, a) =>
              a match {
                case x: NymphArgs => x.copy(workingDir = s)
                case x            => x
            }),
          opt[Int]("port")
            .text("jsonrpc server listened port")
            .abbr("p")
            .action((i, a) =>
              a match {
                case x: NymphArgs => x.copy(jsonrpcPort = i)
                case x            => x
            }),
          opt[String]("host")
            .text("jsonrpc server listened host")
            .abbr("h")
            .action((h, a) =>
              a match {
                case x: NymphArgs => x.copy(jsonrpcHost = h)
                case x            => x
            }),
          opt[String]("service-name")
            .text("jsonrpc service name, which is used as a segment in the service endpoint")
            .action((sn, a) =>
              a match {
                case x: NymphArgs => x.copy(jsonrpcServiceName = sn)
                case x            => x
            }),
          opt[String]("service-version")
            .text("jsonrpc service version, which is used as a segment in the service endpoint")
            .action((v, a) =>
              a match {
                case x: NymphArgs => x.copy(jsonrpcServiceVersion = v)
                case x            => x
            }),
          opt[Int]("snapshot-db-port")
            .text("snapshot db server listened port")
            .action((i, a) =>
              a match {
                case x: NymphArgs => x.copy(snapshotDbPort = i)
                case x            => x
            }),
          opt[Int]("snapshot-db-console-port")
            .text("snapshot db console web server listened port")
            .action((i, a) =>
              a match {
                case x: NymphArgs => x.copy(snapshotDbConsolePort = i)
                case x            => x
            }),
          opt[Unit]("snapshot-db-console")
            .text("start db console web server ?")
            .action((_, a) =>
              a match {
                case x: NymphArgs => x.copy(startSnapshotDbConsole = true)
                case x            => x
            }),
          opt[Int]("node-port")
            .text("p2p node listened port")
            .action((i, a) =>
              a match {
                case x: NymphArgs => x.copy(nodePort = i)
                case x            => x
            }),
          opt[String]("node-ip")
            .text("p2p node listened ip")
            .action((s, a) =>
              a match {
                case x: NymphArgs => x.copy(nodeIp = s)
                case x            => x
            }),
          opt[String]("seeds")
            .text("p2p seed nodes, format is like <ip:port,ip1:port1...>")
            .action((ss, a) =>
              a match {
                case x: NymphArgs => x.copy(seeds = ss.split(",").toVector.map(_.trim))
                case x            => x
            }),
          opt[String]("warrior-nodes")
            .text("the warrior p2p nodes of a nymph, format is like <ip:port,ip1:port2...>")
            .action((ss, a) =>
              a match {
                case x: NymphArgs => x.copy(warriorNodes = ss.split(",").toVector.map(_.trim))
                case x            => x
            })
        )

      cmd("cmd")
        .text("command line tools")
        .children(
          cmd("createAccount")
            .text("create an account by using the input password")
            .action((_, _) => CreateAccountArgs())
            .children(
              opt[String]("pass")
                .text("password of account")
                .abbr("p")
                .required()
                .action {
                  case (x, a @ CreateAccountArgs(_)) => a.copy(pass = x)
                  case (_, a)                        => a
                }
            )
        )
    }
  }

  object implicits extends Implicits
}
