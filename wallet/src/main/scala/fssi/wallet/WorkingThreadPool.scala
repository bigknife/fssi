package fssi.wallet

import java.util.concurrent.Executors

trait WorkingThreadPool {
  private val executorService = Executors.newFixedThreadPool(4)

  def delay(f: => Unit): Unit = executorService.submit(new Runnable {
    override def run(): Unit = f
  })
}

object WorkingThreadPool extends WorkingThreadPool
