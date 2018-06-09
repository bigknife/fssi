package fssi.interpreter.util

import java.util.concurrent.atomic.AtomicLong
import fssi.ast.domain.types.Moment
import scala.collection._

trait MomentPool {

  val maxSize: Int
  val maxElapsedSecond: Int

  private val momentBuf: mutable.ListBuffer[Moment] = mutable.ListBuffer.empty
  private val lastClearTime: AtomicLong             = new AtomicLong(System.currentTimeMillis())

  private def elapsedTime: Long = System.currentTimeMillis() - lastClearTime.get()

  def push(moment: Moment): Boolean = {
    if (size >= maxSize) false
    else {
      momentBuf += moment
      true
    }
  }

  def canPull: Boolean =
    (size >= 0) &&
      ((elapsedTime >= maxElapsedSecond * 1000) ||
        (size >= maxSize))

  def pullOut: Vector[Moment] = {
    val ret = momentBuf.toVector
    momentBuf.clear()
    lastClearTime.set(System.currentTimeMillis())
    ret
  }

  def size: Int = momentBuf.size

}

object MomentPool {
  def newPool(momentSize: Int, elapsedSeconds: Int): MomentPool = new MomentPool {
    override val maxSize: Int          = momentSize
    override val maxElapsedSecond: Int = elapsedSeconds
  }
}
