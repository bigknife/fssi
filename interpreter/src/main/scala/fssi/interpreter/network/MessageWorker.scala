package fssi.interpreter.network

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import fssi.base.Var
import fssi.interpreter.ConsensusHandler
import fssi.interpreter.Setting.DefaultSetting
import fssi.interpreter.scp.SCPEnvelope
import fssi.types.biz.Message
import org.slf4j.LoggerFactory

trait MessageWorker[M <: Message] {
  val messageReceiver: MessageReceiver
  val messageHandler: Message.Handler[M, Unit]
  val workingSlotIndex: BigInt

  private lazy val log = LoggerFactory.getLogger("fssi.interpreter.network.message.worker")

  private lazy val atc = new AtomicInteger(0)
  private lazy val ntc = new AtomicInteger(0)
  private lazy val btc = new AtomicInteger(0)
  private lazy val wtc = new AtomicInteger(0)
  private lazy val applicationT =
    Executors.newSingleThreadExecutor((r: Runnable) => {
      val r1 = new Thread(r, s"mw-app-${atc.getAndIncrement()}")
      r1.setDaemon(true)
      r1
    })
  private lazy val nomT =
    Executors.newSingleThreadExecutor((r: Runnable) => {
      val r1 = new Thread(r, s"mw-nom-${ntc.getAndIncrement()}")
      r1.setDaemon(true)
      r1
    })

  private lazy val ballotT =
    Executors.newSingleThreadExecutor((r: Runnable) => {
      val r1 = new Thread(r, s"mw-blt-${btc.getAndIncrement()}")
      r1.setDaemon(true)
      r1
    })

  private lazy val workerT =
    Executors.newSingleThreadExecutor((r: Runnable) => {
      val r1 = new Thread(r, s"mw-wrk-${wtc.getAndIncrement()}")
      r1.setDaemon(true)
      r1
    })

  private lazy val _workingSlotIndex: Var[BigInt] = Var(workingSlotIndex)

  def incSlotIndex(): Unit = _workingSlotIndex.update(_ + 1)

  def needHandleEnvelope(envelope: SCPEnvelope): Boolean = {
    _workingSlotIndex.map(_ == envelope.value.statement.slotIndex.value).unsafe()
  }

  private def _work(message: M): Unit = {
    type NM = fssi.scp.types.Message.Nomination
    type PM = fssi.scp.types.Message.Prepare
    type CM = fssi.scp.types.Message.Confirm
    type EM = fssi.scp.types.Message.Externalize

    message match {
      case x: Message.ApplicationMessage =>
        applicationT.submit(new Runnable {
          override def run(): Unit =
            try {
              val t0 = System.currentTimeMillis()
              messageHandler(message)
              val t1 = System.currentTimeMillis()
              log.info(s"handle app, time spent: ${t1 - t0} ms")
            } catch {
              case t: Throwable => log.error("handle application message failed", t)
            }
        })
        ()
      case x: SCPEnvelope if x.value.statement.message.isInstanceOf[NM] =>
        nomT.submit(new Runnable {
          override def run(): Unit =
            try {
              val t0     = System.currentTimeMillis()
              val coming = x.value.statement.slotIndex.value
              if (needHandleEnvelope(x)) {
                messageHandler(message)
                val t1 = System.currentTimeMillis()
                log.info(
                  s"handle nom(${x.value.statement.from})@$coming, time spent: ${t1 - t0} ms")
              } else {
                if (log.isDebugEnabled()) {
                  log.debug(
                    s"ignore previous nom message, current=${_workingSlotIndex.unsafe()}, " +
                      s"coming=$coming")
                }
              }

            } catch {
              case t: Throwable => log.error("handle application message failed", t)
            }
        })
        ()
      case x: SCPEnvelope =>
        ballotT.submit(new Runnable {
          override def run(): Unit =
            try {
              val t0     = System.currentTimeMillis()
              val coming = x.value.statement.slotIndex.value
              if (needHandleEnvelope(x)) {
                messageHandler(message)
                val t1 = System.currentTimeMillis()
                log.info(
                  s"handle ${x.messageType}(${x.value.statement.from})@$coming, time spent: ${t1 - t0} ms")
              } else {
                if (log.isDebugEnabled()) {
                  log.debug(
                    s"ignore previous ${x.messageType} message, current=${_workingSlotIndex.unsafe()}, " +
                      s"coming=$coming")
                }
              }

            } catch {
              case t: Throwable => log.error("handle application message failed", t)
            }
        })
        ()
      case _ => throw new RuntimeException(s"can't handle $message in worker")
    }
  }

  /** start work
    *
    */
  def startWork(): Unit = {
    workerT.submit(new Runnable {
      override def run(): Unit = {

        def _loop(): Unit = {
          // handle all application message
          def _loopApp(): Unit = {
            messageReceiver.fetchApplicationMessage() match {
              case Some(x) =>
                _work(x.asInstanceOf[M])
                _loopApp()

              case _ => ()
            }
          }
          _loopApp()

          //handle nom
          messageReceiver.fetchNomination().foreach {
            case (_, Some(env)) => _work(env.asInstanceOf[M])
            case _              => ()
          }

          // handle ballot
          messageReceiver.fetchBallot().foreach {
            case (_, Some(env)) => _work(env.asInstanceOf[M])
            case _              => ()
          }

          _loop()
        }

        try {
          _loop()
        } catch {
          case t: Throwable =>
            log.error("workerT faield, re-working", t)
            _loop()
        }

      }
    })
    ()
  }
}

object MessageWorker {
  def apply[M <: Message](_messageReceiver: MessageReceiver,
                          _messageHandler: Message.Handler[M, Unit],
                          __workingSlotIndex: BigInt): MessageWorker[M] = {
    val m = new MessageWorker[M] {
      override val messageReceiver: MessageReceiver         = _messageReceiver
      override val messageHandler: Message.Handler[M, Unit] = _messageHandler
      override val workingSlotIndex: BigInt                 = __workingSlotIndex
    }

    ConsensusHandler.instance
      .subscribeExternalize(_ => m.incSlotIndex())(DefaultSetting)
      .unsafeRunSync()
    m
  }
}
