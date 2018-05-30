package fssi.ast.domain.exceptions

import fssi.ast.domain.types.{Account, Transaction}

case class UnAffordableTransaction(accountId: Account.ID, transactionId: Transaction.ID)
    extends FSSIException(s"Account(id = $accountId) Can't Afford Transaction(id = $transactionId)")
