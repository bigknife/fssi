package fssi.scp.interpreter

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

trait SCPThreadPool {
  private[interpreter] val executor: ExecutorService = {
    val threadFactory = new ThreadFactory {
      val counter = new AtomicInteger(0)
      override def newThread(r: Runnable): Thread = new Thread(r, s"scp-executor-${counter.getAndIncrement()}")
    }
    Executors.newSingleThreadExecutor(threadFactory)
  }
}

object SCPThreadPool extends SCPThreadPool {
  def submit(task: Runnable): Unit = executor.submit(task)
}
