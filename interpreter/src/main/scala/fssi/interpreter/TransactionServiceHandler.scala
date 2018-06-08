package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types.Transaction.{InvokeContract, PublishContract, Transfer}
import fssi.ast.domain.types._

class TransactionServiceHandler extends TransactionService.Handler[Stack] {
  override def randomTransactionID(): Stack[Transaction.ID] = Stack {
    Transaction.ID(java.util.UUID.randomUUID().toString.replace("-", ""))
  }

  override def createTransferWithoutSign(id: Transaction.ID,
                                         from: String,
                                         to: String,
                                         amount: Long): Stack[Transaction.Transfer] = Stack {
    Transfer(
      id,
      Account.ID(from),
      Account.ID(to),
      Token(amount, Token.Unit.Sweet),
      Signature.Empty,
      Transaction.Status.Init(id)
    )
  }

  override def createPublishContractWithoutSign(
      id: Transaction.ID,
      accountId: String,
      name: String,
      version: String,
      contract: Contract.UserContract): Stack[PublishContract] =
    Stack {
      PublishContract(
        id,
        Account.ID(accountId),
        contract,
        Signature.Empty,
        Transaction.Status.Init(id)
      )
    }

  override def createRunContractWithoutSign(id: Transaction.ID,
                                            accountId: String,
                                            name: String,
                                            version: String,
                                            function: String,
                                            params: Contract.Parameter): Stack[InvokeContract] =
    Stack {
      InvokeContract(
        id,
        Account.ID(accountId),
        Contract.Name(name),
        Contract.Version(version),
        Contract.Function(function),
        params,
        Signature.Empty,
        Transaction.Status.Init(id)
      )
    }

  override def createMoment(transaction: Transaction,
                            statesChange: StatesChange,
                            oldStatesHash: BytesValue,
                            newStatesHash: BytesValue): Stack[Moment] = Stack {
    Moment(
      statesChange.previous,
      transaction,
      statesChange.current,
      oldStatesHash,
      newStatesHash
    )
  }
}

object TransactionServiceHandler {
  trait Implicits {
    implicit val transactionServiceHandler: TransactionServiceHandler =
      new TransactionServiceHandler
  }

  object implicits extends Implicits
}
