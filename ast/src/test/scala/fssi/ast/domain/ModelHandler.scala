package fssi.ast.domain

trait ModelHandler
    extends AccountServiceHandler.Implicits
    with CryptoServiceHandler.Implicits
    with LogServiceHandler.Implicits
    with MonitorServiceHandler.Implicits
    with NetworkServiceHandler.Implicits
    with AccountSnapshotHandler.Implicits
    with TransactionServiceHandler.Implicits
    with TransactionStoreHandler.Implicits
    with ConsensusEngineHandler.Implicits
    with ContractServiceHandler.Implicits
    with ContractStoreHandler.Implicits
    with LedgerStoreHandler.Implicits
    with bigknife.sop.effect.error.ErrorMInstance

object ModelHandler extends ModelHandler
