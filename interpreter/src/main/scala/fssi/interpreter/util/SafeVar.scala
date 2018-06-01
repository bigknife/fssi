package fssi.interpreter.util

trait SafeVar[A] {outter =>
  @volatile
  private var a: Option[A] = None

  private def saveUpdate(a: A): Unit = synchronized {
    outter.a = Some(a)
  }

  def apply(): Option[A] = a
  def unsafe(): A = a.get

  def := (a: A): Unit = saveUpdate(a)
}

