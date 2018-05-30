package fssi.ast.domain

trait ModelHandler
    extends AccountServiceHandler.Implicits
    with CryptoServiceHandler.Implicits
    with LogServiceHandler.Implicits
    with MonitorServiceHandler.Implicits
    with NetworkServiceHandler.Implicits
    with AccountStoreHandler.Implicits
    with TransactionServiceHandler.Implicits
    with TransactionStoreHandler.Implicits
    with bigknife.sop.effect.error.ErrorMInstance

object ModelHandler extends ModelHandler
