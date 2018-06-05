package fssi.contract

/**
  * Contract invocations cost
  *
  * @param throwCost 抛出异常
  * @param allocationCost 分配空间
  * @param jumpCost 循环/条件跳转
  * @param methodCallCost 方法调用
  */
case class ContractCost(
    throwCost: Long,
    allocationCost: Long,
    jumpCost: Long,
    methodCallCost: Long
)
