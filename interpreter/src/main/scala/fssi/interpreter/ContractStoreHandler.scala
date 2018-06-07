package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class ContractStoreHandler extends ContractStore.Handler[Stack] {
  override def findContract(name: Contract.Name,
                            version: Contract.Version): Stack[Option[Contract]] = Stack {
    import Contract.inner._
    (name, version) match {
      case (TransferContract.name, TransferContract.version) => Some(TransferContract)
      case (PublishContract.name, PublishContract.version)   => Some(PublishContract)
      case (Contract.Name(n), Contract.Version(v))           => None //todo: find from ledger or snapshot
      case _ => None
    }
  }
}
object ContractStoreHandler {
  trait Implicits {
    implicit val contractStoreHandler: ContractStoreHandler = new ContractStoreHandler
  }
  object implicits extends Implicits
}
