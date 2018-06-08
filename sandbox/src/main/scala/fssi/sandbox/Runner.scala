package fssi.sandbox

import _root_.java.nio.file._

import fssi.contract.{ContractCost, States}

import scala.util.Try

/**
  * Runner for smart contracts
  */
trait Runner {

  /**
    * run and instrument the contract, we assuming that the arguments are all of type String.
    * @param p contract path in which the jar was extracted
    * @param clazzName contract class name(full identified)
    * @param methodName contract method name
    * @param invoker invoker account id
    * @param states now states
    * @param args contract method arguments except sendId and states
    * @return
    */
  def runAndInstrument(p: Path,
                       clazzName: String,
                       methodName: String,
                       invoker: String,
                       states: States,
                       args: Seq[AnyRef]): Either[Throwable, (States, ContractCost)] = {
    Try {
      val ccl                   = new ContractClassLoader(p)
      val cls                   = ccl.findClass(clazzName)
      val clss                  = args.map(_ => classOf[String])
      val statesCls             = classOf[fssi.contract.java.States]
      val fullClasses           = Seq(classOf[String], statesCls) ++ clss
      val method                = cls.getMethod(methodName, fullClasses: _*)
      val inst                  = cls.newInstance()
      val javaStates            = fssi.contract.java.States.fromScala(states)
      val fullArgs: Seq[AnyRef] = Seq.empty ++ Seq(invoker, javaStates) ++ args
      val nowStates =
        method.invoke(inst, fullArgs: _*).asInstanceOf[fssi.contract.java.States].asScala()
      val cost = ContractCost(
        java.CostAccounting.throwCost(),
        java.CostAccounting.allocationCost(),
        java.CostAccounting.jumpCost(),
        java.CostAccounting.methodCallCost()
      )
      (nowStates, cost)
    }.toEither
  }
}

object Runner extends Runner
