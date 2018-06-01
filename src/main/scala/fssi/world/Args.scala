package fssi.world

sealed trait Args

object Args {
  case class EmptyArgs() extends Args

  case class NymphArgs(
      port: Int = 8080,
      host: String = "0.0.0.0",
      serviceName: String = "nymph",
      serviceVersion: String = "v1"
  ) extends Args

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
          opt[Int]("port")
            .text("jsonrpc server listened port")
            .abbr("p")
            .action((i, a) =>
              a match {
                case x: NymphArgs => x.copy(port = i)
                case x            => x
            }),
          opt[String]("host")
            .text("jsonrpc server listened host")
            .abbr("h")
            .action((h, a) =>
              a match {
                case x: NymphArgs => x.copy(host = h)
                case x            => x
            }),
          opt[String]("service-name")
            .text("jsonrpc service name, which is used as a segment in the service endpoint")
            .action((sn, a) =>
              a match {
                case x: NymphArgs => x.copy(serviceName = sn)
                case x            => x
            }),
          opt[String]("service-version")
            .text("jsonrpc service version, which is used as a segment in the service endpoint")
            .action((v, a) =>
              a match {
                case x: NymphArgs => x.copy(serviceVersion = v)
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
                  case (x, a@CreateAccountArgs(_)) => a.copy(pass = x)
                  case (_, a) => a
                }
            )
        )
    }
  }

  object implicits extends Implicits
}
