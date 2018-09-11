package fssi
package sandbox
package counter
import org.slf4j.{Logger, LoggerFactory}

class CostCounter {
  private var throwCost: Long      = 0
  private var jumpCost: Long       = 0
  private var allocationCost: Long = 0
  private var methodCallCost: Long = 0
}

object CostCounter {

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  private val BASELINE_ALLOC_KILL_THRESHOLD       = 1024 * 1024
  private val BASELINE_JUMP_KILL_THRESHOLD        = 1024
  private val BASELINE_METHOD_CALL_KILL_THRESHOLD = 1024 * 1024
  private val BASELINE_THROW_KILL_THRESHOLD       = 50

  private val COST_UNIT = 1

  private val threadLocal: ThreadLocal[CostCounter] = new ThreadLocal[CostCounter] {
    override def initialValue(): CostCounter = new CostCounter
  }

  def recordThrow(): Unit = {
    val thread = Thread.currentThread()
    logger.debug(s"record throw for [$thread]")
    val costCounter      = threadLocal.get()
    val throwCostCounter = costCounter.throwCost + COST_UNIT
    if (throwCostCounter > BASELINE_THROW_KILL_THRESHOLD) {
      logger.error(s"Thread [$thread] terminated for excessive exception throwing")
      throw new ThreadDeath
    } else {
      costCounter.throwCost = throwCostCounter
    }
  }

  def recordArrayAllocation(length: Int, multiplier: Int): Unit = {
    val thread = Thread.currentThread()
    logger.debug(s"record array allocation: $length * $multiplier for [$thread]")
    val costCounter            = threadLocal.get()
    val arrayAllocationCounter = costCounter.allocationCost + length * multiplier * COST_UNIT
    if (arrayAllocationCounter > BASELINE_ALLOC_KILL_THRESHOLD) {
      logger.error(s"Thread [$thread] terminated for overallocation")
      throw new ThreadDeath
    } else {
      costCounter.allocationCost = arrayAllocationCounter
    }
  }

  def recordAllocation(className: String): Unit = {
    val thread = Thread.currentThread()
    logger.debug(s"record object allocation for [$thread]")
    val costCounter       = threadLocal.get()
    val allocationCounter = costCounter.allocationCost + COST_UNIT
    if (allocationCounter > BASELINE_ALLOC_KILL_THRESHOLD) {
      logger.error(s"Thread [$thread] terminated for overallocation")
      throw new ThreadDeath
    } else {
      costCounter.allocationCost = allocationCounter
    }
  }

  def recordJump(): Unit = {
    val thread = Thread.currentThread()
    logger.debug(s"record jump for [$thread]")
    val costCounter = threadLocal.get()
    val jumpCounter = costCounter.jumpCost + COST_UNIT
    if (jumpCounter > BASELINE_JUMP_KILL_THRESHOLD) {
      logger.error(s"Thread [$thread] terminated for excessive jump")
      throw new ThreadDeath
    } else {
      costCounter.jumpCost = jumpCounter
    }
  }

  def recordMethodCall(): Unit = {
    val thread = Thread.currentThread()
    logger.debug(s"record method call for [$thread]")
    val costCounter       = threadLocal.get()
    val methodCallCounter = costCounter.methodCallCost + COST_UNIT
    if (methodCallCounter > BASELINE_METHOD_CALL_KILL_THRESHOLD) {
      logger.error(s"Thread [$thread] terminated for excessive method calling")
      throw new ThreadDeath
    } else {
      costCounter.methodCallCost = methodCallCounter
    }
  }

  def throwCost: Long      = threadLocal.get().throwCost
  def jumpCost: Long       = threadLocal.get().jumpCost
  def allocationCost: Long = threadLocal.get().allocationCost
  def methodCallCost: Long = threadLocal.get().methodCallCost
}
