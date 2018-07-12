package fssi.world.handler

import fssi.ast.domain.components.Model.Op
import fssi.ast.domain.types.{BytesValue, DataPacket}
import fssi.ast.usecase.Warrior
import fssi.interpreter.runner
import fssi.world.Args
import org.slf4j.{Logger, LoggerFactory}

class WarriorHandler extends ArgsHandler[Args.WarriorArgs] {

  override def logbackConfigResource(args: Args.WarriorArgs): String =
    (args.verbose, args.colorfulLog) match {
      case (true, true)   => "/logback-warrior-verbose-color.xml"
      case (true, false)  => "/logback-warrior-verbose.xml"
      case (false, false) => "/logback-warrior.xml"
      case (false, true)  => "/logback-warrior-color.xml"
    }

  override def run(args: Args.WarriorArgs): Unit = {

    WarriorHandler.startP2PNode(args)
    Thread.currentThread().join()
  }
}

object WarriorHandler {
  val logger: Logger       = LoggerFactory.getLogger(getClass)
  val warrior: Warrior[Op] = Warrior[Op]

  // all p2p message is delegated to P2PDataPacketHandler to handle.
  object dataPacketHandler extends P2PDataPacketHandler

  def p2pHandler(args: Args.WarriorArgs): DataPacket => Unit =
    dataPacketHandler.handle(_, warrior, args)

  // start p2p node
  def startP2PNode(args: Args.WarriorArgs): Unit = {
    println("Input current warrior node bound account password:")
    val console = System.console()
    val password = if (console == null) scala.io.StdIn.readLine().getBytes("utf-8") else console.readPassword().map(_.toByte)
    val argsFull = args.copy(pass = password)

    val p = warrior.startup(BytesValue(argsFull.boundAccountPublicKey).hex,
                            argsFull.nodeIp,
                            argsFull.nodePort,
                            argsFull.seeds,
                            p2pHandler(argsFull))


    runner.runIOAttempt(p, argsFull.toSetting).unsafeRunSync() match {
      case Left(t)  => logger.error("start p2p node failed", t)
      case Right(v) => logger.info("p2p node start, id {}", v.toString)
    }
    // add shutdown hook
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      runner.runIOAttempt(warrior.shutdown(), argsFull.toSetting).unsafeRunSync() match {
        case Left(t)  => logger.warn("shutdown p2p node failed", t)
        case Right(_) => logger.debug("p2p node shut down.")
      }
    }))
  }
}
