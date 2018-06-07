package fssi.sandbox
import org.slf4j.LoggerFactory

/**
  * a byte code invoking times counter
  */
class CostAccounting {
  private var throwCost: Long = 0
  private var jumpCost: Long = 0
  private var allocationCost: Long = 0
  private var methodCallCost: Long = 0
}

object CostAccounting {
  private val logger = LoggerFactory.getLogger(getClass)

  // the threshold should be optimized according to reality
  private val BASELINE_ALLOC_KILL_THRESHOLD = 1024 * 1024
  private val BASELINE_JUMP_KILL_THRESHOLD = 1024
  private val BASELINE_METHOD_CALL_KILL_THRESHOLD = 1024 * 1024
  private val BASELINE_THROW_KILL_THRESHOLD = 50

  private val COST_UNIT: Long = 1

  val tl: ThreadLocal[CostAccounting] = new ThreadLocal[CostAccounting]

  def recordThrow(): Unit = {
    val currentThread = Thread.currentThread()
    if(logger.isDebugEnabled()) logger.debug(s"record throw for $currentThread")
    val costAccounting = currentThreadCostAccounting()
    val newThrowCost = costAccounting.throwCost + COST_UNIT
    if (newThrowCost > BASELINE_THROW_KILL_THRESHOLD) {
      if(logger.isDebugEnabled()) logger.debug(s"Contract $currentThread terminated for excessive exception throwing")
      throw new ThreadDeath
    }
    else costAccounting.throwCost = newThrowCost
  }

  def recordArrayAllocation(length: Int, multiplier: Int): Unit = {
    val currentThread = Thread.currentThread()
    if (logger.isDebugEnabled()) logger.debug(s"record array allocation: $length * $multiplier for $currentThread")
    val costAccounting = currentThreadCostAccounting()
    val newAllocationCost = costAccounting.allocationCost + (length * multiplier) * COST_UNIT
    if (newAllocationCost > BASELINE_ALLOC_KILL_THRESHOLD) {
      if(logger.isDebugEnabled()) logger.debug(s"Contract $currentThread terminated for overallocation")
      throw new ThreadDeath
    }
    else costAccounting.allocationCost = newAllocationCost

  }

  def recordAllocation(className: String): Unit = {
    val currentThread = Thread.currentThread()
    if (logger.isDebugEnabled()) logger.debug(s"record object allocation: $className for $currentThread")
    val costAccounting = currentThreadCostAccounting()
    val newAllocationCost = costAccounting.allocationCost + COST_UNIT
    if (newAllocationCost > BASELINE_ALLOC_KILL_THRESHOLD) {
      if(logger.isDebugEnabled()) logger.debug(s"Contract $currentThread terminated for overallocation")
      throw new ThreadDeath
    }
    else costAccounting.allocationCost = newAllocationCost
  }

  def recordJump(): Unit = {
    val currentThread = Thread.currentThread()
    if (logger.isDebugEnabled) logger.debug(s"record jump")
    val costAccounting = currentThreadCostAccounting()
    val newJumpCost = costAccounting.jumpCost + COST_UNIT
    if (newJumpCost > BASELINE_JUMP_KILL_THRESHOLD) {
      if(logger.isDebugEnabled()) logger.debug(s"Contract $currentThread terminated for excessive use of looping")
      throw new ThreadDeath
    }
    else costAccounting.jumpCost = newJumpCost
  }

  def recordMethodCall(): Unit = {
    val currentThread = Thread.currentThread()
    if(logger.isDebugEnabled()) logger.debug(s"record method call")
    val costAccounting = currentThreadCostAccounting()
    val newMethodCallCost = costAccounting.methodCallCost + COST_UNIT
    if (newMethodCallCost > BASELINE_METHOD_CALL_KILL_THRESHOLD) {
      if(logger.isDebugEnabled()) logger.debug(s"Contract $currentThread terminated for excessive use of method calling")
      throw new ThreadDeath
    }
    else costAccounting.methodCallCost = newMethodCallCost
  }

  def throwCost(): Long = currentThreadCostAccounting().throwCost
  def allocationCost(): Long = currentThreadCostAccounting().allocationCost
  def methodCallCost(): Long = currentThreadCostAccounting().methodCallCost
  def jumpCost(): Long = currentThreadCostAccounting().jumpCost

  private def currentThreadCostAccounting(): CostAccounting = {
    Option(tl.get()).orElse {
      val costAccounting = new CostAccounting
      tl.set(costAccounting)
      Some(costAccounting)
    }.get
  }

}