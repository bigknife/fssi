package fssi
package interpreter
package scp

import org.slf4j._
import java.util.concurrent._
import scala.util._

sealed trait SCPExecutionService {
  private val log = LoggerFactory.getLogger(getClass)

  private val tc: ThreadFactory = new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r)
      t.setDaemon(true)
      t.setName("scp-consensus-thread")
      t
    }
  }
  private val es = Executors.newSingleThreadExecutor(tc)

  def submit(t: => Unit): Unit = {
    es.submit(new Runnable {
      override def run(): Unit = {
        Try {
          t
        } match {
          case Success(_) =>
            log.info("executed consensus task successfully")
          case Failure(exception) =>
            log.error("executing consensus task failed", exception)
        }
      }
    })
    ()
  }

  def repeat(times: Int)(t: Int => Unit): Unit = {
    for (i <- 0 until times) {
      submit( t(i))
    }
  }

}

object SCPExecutionService extends SCPExecutionService
