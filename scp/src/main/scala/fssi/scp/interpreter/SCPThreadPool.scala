package fssi.scp.interpreter

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

trait SCPThreadPool {

  def threadFactory(name: String) = new ThreadFactory {
    val counter = new AtomicInteger(0)
    override def newThread(r: Runnable): Thread = new Thread(r, s"scp-$name-${counter.getAndIncrement()}")
  }

  private[interpreter] val broadcastExecutor: ExecutorService = {
    Executors.newSingleThreadExecutor(threadFactory("broadcast"))
  }

  private[interpreter] val executor: ExecutorService = {
    Executors.newSingleThreadExecutor(threadFactory("executor"))
  }
}

object SCPThreadPool extends SCPThreadPool {
  def submit(task: Runnable): Unit = executor.submit(task)
  def broadcast(task: Runnable): Unit = broadcastExecutor.submit(task)
}
