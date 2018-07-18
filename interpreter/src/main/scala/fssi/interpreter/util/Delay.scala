package fssi.interpreter.util

trait Delay {
  def apply(timeout: Long)(v: => Unit): Unit = {
    new java.util.Timer("delay", true).schedule(new java.util.TimerTask {
      override def run(): Unit = v
    }, timeout)
  }
}
