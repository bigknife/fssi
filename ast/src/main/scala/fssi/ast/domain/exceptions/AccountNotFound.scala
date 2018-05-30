package fssi.ast.domain.exceptions

import fssi.ast.domain.types.Account

case class AccountNotFound(id: Account.ID) extends
  FSSIException(s"Account(id = $id) Not Found Yet! Please Retry Later.")
