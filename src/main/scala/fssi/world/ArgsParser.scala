package fssi.world

import fssi.world.Args._
import scopt.OptionDef

trait ArgsParser {
  implicit object parser extends _root_.scopt.OptionParser[Args]("fssi") {
    head("fssi", "0.0.1")
    help("help").abbr("h").text("print this help documents")

    def workingDir: OptionDef[java.io.File, Args] =
      opt[java.io.File]("working-dir")
        .text("nymph working dir")
        .action((s, a) =>
          a match {
            case x: NymphArgs   => x.copy(workingDir = s.getAbsolutePath)
            case x: WarriorArgs => x.copy(workingDir = s.getAbsolutePath)
            case x              => x
        })

    def snapshotDbPort: OptionDef[Int, Args] =
      opt[Int]("snapshot-db-port")
        .text("snapshot db server listened port")
        .action((i, a) =>
          a match {
            case x: NymphArgs   => x.copy(snapshotDbPort = i)
            case x: WarriorArgs => x.copy(snapshotDbPort = i)
            case x              => x
        })

    def verbose: OptionDef[Unit, Args] =
      opt[Unit]("verbose")
        .text("print all logs")
        .action((_, a) =>
          a match {
            case x: NymphArgs   => x.copy(verbose = true)
            case x: WarriorArgs => x.copy(verbose = true)
            case x              => x
        })

    def colorfulLog: OptionDef[Boolean, Args] =
      opt[Boolean]("color")
        .text("turn on/off the colorful log")
        .action((b, a) =>
          a match {
            case x: NymphArgs   => x.copy(colorfulLog = b)
            case x: WarriorArgs => x.copy(colorfulLog = b)
            case x              => x
        })

    def snapshotDbConsolePort: OptionDef[Int, Args] =
      opt[Int]("snapshot-db-console-port")
        .text("snapshot db console web server listened port")
        .action((i, a) =>
          a match {
            case x: NymphArgs   => x.copy(snapshotDbConsolePort = i)
            case x: WarriorArgs => x.copy(snapshotDbConsolePort = i)
            case x              => x
        })

    def snapshotDbConsole: OptionDef[Unit, Args] =
      opt[Unit]("snapshot-db-console")
        .text("start db console web server ?")
        .action((_, a) =>
          a match {
            case x: NymphArgs   => x.copy(startSnapshotDbConsole = true)
            case x: WarriorArgs => x.copy(startSnapshotDbConsole = true)
            case x              => x
        })

    def nodePort: OptionDef[Int, Args] =
      opt[Int]("node-port")
        .text("p2p node listened port")
        .action((i, a) =>
          a match {
            case x: NymphArgs   => x.copy(nodePort = i)
            case x: WarriorArgs => x.copy(nodePort = i)
            case x              => x
        })

    def nodeIp: OptionDef[String, Args] =
      opt[String]("node-ip")
        .text("p2p node listened ip")
        .action((s, a) =>
          a match {
            case x: NymphArgs   => x.copy(nodeIp = s)
            case x: WarriorArgs => x.copy(nodeIp = s)
            case x              => x
        })

    def seeds: OptionDef[String, Args] =
      opt[String]("seeds")
        .text("p2p seed nodes, format is like <ip:port,ip1:port1...>")
        .action((ss, a) =>
          a match {
            case x: NymphArgs   => x.copy(seeds = ss.split(",").toVector.map(_.trim))
            case x: WarriorArgs => x.copy(seeds = ss.split(",").toVector.map(_.trim))
            case x              => x
        })

    cmd("nymph")
      .text("run nymph jsonrpc server")
      .action((_, _) => NymphArgs())
      .children(
        workingDir,
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
        snapshotDbPort,
        snapshotDbConsolePort,
        snapshotDbConsole,
        nodePort,
        nodeIp,
        seeds,
        opt[String]("warrior-nodes")
          .text("the warrior p2p nodes of a nymph, format is like <ip:port,ip1:port2...>")
          .action((ss, a) =>
            a match {
              case x: NymphArgs => x.copy(warriorNodes = ss.split(",").toVector.map(_.trim))
              case x            => x
          }),
        verbose,
        colorfulLog
      )

    cmd("warrior")
      .text("run warrior validation server")
      .action((_, _) => WarriorArgs())
      .children(
        workingDir,
        snapshotDbPort,
        snapshotDbConsole,
        snapshotDbConsolePort,
        nodeIp,
        nodePort,
        seeds,
        verbose,
        colorfulLog
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
          ),
        cmd("createTransfer")
          .text("create a transfer protocol represented with jsonrpc")
          .action((_, _) => CreateTransferArgs())
          .children(
            opt[String]("account-id")
              .text("account id from whom the token is transferred")
              .required()
              .action((id, a) =>
                a match {
                  case x: CreateTransferArgs => x.copy(accountId = id)
                  case x                     => x
              }),
            opt[String]("transfer-to")
              .text("account id to whom the token is transferred")
              .required()
              .action((id, a) =>
                a match {
                  case x: CreateTransferArgs => x.copy(transferTo = id)
                  case x                     => x
              }),
            opt[Long]("amount")
              .text("the amount to be transferred, the unit is `Sweet`")
              .required()
              .validate(x =>
                scala.util.Try { x > 0 }.toEither.right.map(_ => ()).left.map(_.getMessage))
              .action((amount, a) =>
                a match {
                  case x: CreateTransferArgs => x.copy(amount = amount)
                  case x                     => x
              }),
            opt[String]("private-key")
              .text("the account's private key, used to sign the transfer transaction")
              .required()
              .action((p, a) =>
                a match {
                  case x: CreateTransferArgs => x.copy(privateKey = p)
                  case x                     => x
              }),
            opt[String]("password")
              .text("the password to decrypt the private key")
              .required()
              .action((p, a) =>
                a match {
                  case x: CreateTransferArgs => x.copy(password = p)
                  case x                     => x
              }),
            opt[String]("iv")
              .text("the iv spec to decrypt the private key")
              .required()
              .action((i, a) =>
                a match {
                  case x: CreateTransferArgs => x.copy(iv = i)
                  case x                     => x
              }),
            opt[String]("output-format")
              .text("json output format, one of the tree: 1. nospace, 2. space2, 3. space4. default is 1.")
              .action((f, a) =>
                a match {
                  case x: CreateTransferArgs => x.copy(outputFormat = f)
                  case x                     => x
              })
          ),
        cmd("compileContract")
          .text("compile a contract project")
          .action((_, _) => CompileContractArgs())
          .children(
            opt[java.io.File]("project-dir")
              .text("the project root directory, which should include src and META-INF")
              .action((f, a) =>
                a match {
                  case x: CompileContractArgs => x.copy(projectDir = f.getAbsolutePath)
                  case x                      => x
              }),
            opt[String]("output-format")
              .text("the contract output format, should be `jar`, `hex` or `base64`")
              .validate(f =>
                if (f == "jar" || f == "hex" || f == "base64") Right(())
                else Left("the contract output format, should be `jar`, `hex` or `base64`"))
              .required()
              .action((f, a) =>
                a match {
                  case x: CompileContractArgs => x.copy(outputFormat = f)
                  case x                      => x
              }),
            opt[java.io.File]("output-file")
              .text("the path of contract file saved")
              .required()
              .action((f, a) =>
                a match {
                  case x: CompileContractArgs => x.copy(outputFile = f.getAbsolutePath)
                  case x                      => x
              })
          )
      )
  }
}
