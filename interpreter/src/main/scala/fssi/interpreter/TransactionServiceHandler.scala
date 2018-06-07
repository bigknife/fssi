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

  override def createPublishContractWithoutSign(id: Transaction.ID,
                                                accountId: String,
                                                name: String,
                                                version: String,
                                                contract: Contract): Stack[PublishContract] =
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
                                            params: Contract.Parameter): Stack[InvokeContract] =
    Stack {
      InvokeContract(
        id,
        Account.ID(accountId),
        Contract.Name(name),
        Contract.Version(version),
        params,
        Signature.Empty,
        Transaction.Status.Init(id)
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
