package fssi.interpreter.network

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}

import fssi.base.Var
import fssi.interpreter.scp.SCPEnvelope
import fssi.scp.interpreter.NodeStoreHandler
import fssi.scp.types.NodeID
import fssi.types.biz.Message
import fssi.types.biz.Message.ApplicationMessage
import org.slf4j.LoggerFactory

/** Message Receiver
  * receive
  */
trait MessageReceiver {
  private val t                                                        = Executors.newSingleThreadExecutor()
  private val applicationMessagePool: Var[Vector[ApplicationMessage]]  = Var(Vector.empty)
  private val nomMessagePool: Var[Map[NodeID, Vector[SCPEnvelope]]]    = Var(Map.empty)
  private val ballotMessagePool: Var[Map[NodeID, Vector[SCPEnvelope]]] = Var(Map.empty)

  // monitor thread is used to monitor the message pool
  private val tc = new AtomicInteger(0)
  private val monitorThread = Executors.newSingleThreadScheduledExecutor(new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r, s"mr-mon-${tc.getAndIncrement()}")
      t.setDaemon(true)
      t
    }
  })
  private lazy val log = LoggerFactory.getLogger("fssi.interpreter.network.message.receiver")

  // received message counter
  private val applicationMessageReceivedCounter: Var[Long]            = Var(0)
  private val consensusMessageReceivedCounter: Var[Map[NodeID, Long]] = Var(Map.empty)

  /**
    * receive messages. in an alone thread.
    * @param msg message
    */
  def receive(msg: Message): Unit = {
    t.submit(new Runnable {
      override def run(): Unit = {
        msg match {
          case x: ApplicationMessage =>
            logTimeCost(s"----- [RECV] app message", "    pushed into message pool, ") {
              applicationMessagePool.synchronized {
                applicationMessagePool.update(_ :+ x)
                applicationMessageReceivedCounter.update(_ + 1)
                ()
              }
            }

          case x: SCPEnvelope
              if x.value.statement.message.isInstanceOf[fssi.scp.types.Message.Nomination] =>
            logTimeCost(s"------ [RECV] ${x.messageType}@${x.slotIndex} message " +
                          s"from ${x.value.statement.from}",
                        "    pushed into message pool, ") {
              nomMessagePool.synchronized {
                nomMessagePool.update { m =>
                  val nodeId = x.value.statement.from

                  consensusMessageReceivedCounter.map { m =>
                    m + (nodeId -> (m.getOrElse(nodeId, 0L) + 1))
                  }

                  if (m.contains(nodeId)) {
                    m + (nodeId     -> (m(nodeId) :+ x))
                  } else Map(nodeId -> Vector(x))
                }
                ()
              }
            }

          case x: SCPEnvelope =>
            logTimeCost(s"------ [RECV] ${x.messageType}@${x.slotIndex} message " +
                          s"from ${x.value.statement.from}",
                        "    pushed into message pool, ") {
              ballotMessagePool.synchronized {
                ballotMessagePool.update { m =>
                  val nodeId = x.value.statement.from

                  consensusMessageReceivedCounter.map { m =>
                    m + (nodeId -> (m.getOrElse(nodeId, 0L) + 1))
                  }

                  if (m.contains(nodeId)) {
                    m + (nodeId     -> (m(nodeId) :+ x))
                  } else Map(nodeId -> Vector(x))
                }
                ()
              }
            }

          case _ => throw new RuntimeException("no such kind of message")
        }
      }
    })
    ()
  }

  /**
    * fetch cached application message.
    * client should iterate this method util it returns none
    * @return
    */
  def fetchApplicationMessage(): Option[ApplicationMessage] = {
    applicationMessagePool.synchronized {
      val r = applicationMessagePool.map(_.headOption).unsafe()
      applicationMessagePool.update { m =>
        m.drop(1)
      }
      r
    }
  }

  /**
    * fetch nomination message
    * @return
    */
  def fetchNomination(): Map[NodeID, Option[SCPEnvelope]] = {
    nomMessagePool.synchronized {
      val r = nomMessagePool
        .map { m =>
          m.map {
            case (nodeId, envelopes) =>
              val sorted = envelopes.sortWith {
                case (x0, x1) =>
                  (x0.value.statement.message, x1.value.statement.message) match {
                    case (a @ fssi.scp.types.Message.Nomination(_, _),
                          b @ fssi.scp.types.Message.Nomination(_, _)) =>
                      // the newer is arranged after the older
                      b.isNewerThan(a)

                    case _ => throw new RuntimeException("both should be nomination message")
                  }
              }

              nodeId -> sorted.lastOption
          }
        }
        .unsafe()

      // when got a result of nodeId, then clear all nom of from this node
      nomMessagePool := Map.empty
      r
    }
  }

  /**
    * fetch ballot message
    * @return
    */
  def fetchBallot(): Map[NodeID, Option[SCPEnvelope]] = {
    ballotMessagePool.synchronized {
      type BM = fssi.scp.types.Message.BallotMessage
      val r = ballotMessagePool
        .map { m =>
          m.map {
            case (nodeId, envelopes) =>
              val sorted = envelopes.sortWith {
                case (e1, e2) =>
                  e1.value.statement.message match {
                    case x1: BM =>
                      e2.value.statement.message match {
                        case x2: BM =>
                          NodeStoreHandler.instance.isNewerBallotMessage(x2, x1)
                        case _ => throw new RuntimeException("both should be ballot message")
                      }
                    case _ => throw new RuntimeException("both should be ballot message")
                  }
              }

              nodeId -> sorted.lastOption
          }
        }
        .unsafe()

      // when got a result of nodeId, then clear all nom of from this node
      ballotMessagePool := Map.empty
      r
    }
  }

  monitorThread.scheduleAtFixedRate(
    () => {

      consensusMessageReceivedCounter.foreach { m =>
        m.foreach {
          case (nodeId, cnt) if cnt > 0 =>
            log.info(s"received consensus message count: $nodeId = $cnt")
          case _ =>
        }
      }

      nomMessagePool.foreach { m =>
        m.foreach {
          case (nodeId, envelpes) =>
            log.info(s"cached nom message count: $nodeId = ${envelpes.size}")
        }
      }

      ballotMessagePool.foreach { m =>
        m.foreach {
          case (nodeId, envelpes) =>
            log.info(s"cached ballot message count: $nodeId = ${envelpes.size}")
        }
      }

      applicationMessageReceivedCounter.foreach { a =>
        if (a > 0) log.debug(s"received application message count: $a")
        else ()
      }

      applicationMessagePool.foreach { a =>
        if (a.nonEmpty) log.debug(s"applicationPool cached message count: ${a.size}")
        else ()
      }
    },
    5,
    5,
    TimeUnit.SECONDS
  )

  private def logTimeCost(startWords: String, endWords: String)(f: => Unit): Unit = {
    if (log.isDebugEnabled) log.debug(s"$startWords")
    val start = System.currentTimeMillis()
    f
    val end = System.currentTimeMillis()
    if (log.isDebugEnabled) log.debug(s"$endWords, time spent: ${end - start} ms")
  }
}

object MessageReceiver {
  def apply(): MessageReceiver = new MessageReceiver {}
}
