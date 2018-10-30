package fssi
package interpreter
import fssi.ast.Contract
import fssi.types.biz.{Receipt, Transaction}

class ContractHandler extends Contract.Handler[Stack] {

  val sandbox = new fssi.sandbox.SandBox

  /** check runtime, if not acceptable, throw exception
    */
  override def assertRuntime(): Stack[Unit] = Stack {
    sandbox.checkRunningEnvironment match {
      case Right(_) => ()
      case Left(e)  => throw e
    }
  }

  override def initializeRuntime(): Stack[Unit] = Stack {}

  override def closeRuntime(): Stack[Unit] = Stack {}

  override def runTransaction(transaction: Transaction): Stack[Receipt] = Stack {
    transaction match {
      case transfer: Transaction.Transfer => ???
      case deploy: Transaction.Deploy     => ???
      case run: Transaction.Run           => ???
    }
  }
}

object ContractHandler {
  val instance = new ContractHandler

  trait Implicits {
    implicit val contractHandler: ContractHandler = instance
  }
}
