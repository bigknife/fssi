package fssi.ast.usecase

import bigknife.sop._, implicits._
import implicits._
import fssi.ast.domain.components.Model
import fssi.ast.domain.exceptions._
import fssi.ast.domain.types._

/** implementation of core use cases of NymphUseCases
  *
  */
trait Nymph[F[_]] extends NymphUseCases[F] {
  val model: Model[F]

  import model._

  private val nothingToDo: SP[F, Unit] = ().pureSP[F]

  /**
    * uc1. enroll, become a member of the chain who's identified by an account.
    *
    * @param rand random string to encrypt the private key of the account being created.
    * @return account
    */
  override def register(rand: String): SP[F, Account] = {
    // first we create an account
    val createAccount: SP[F, Account] = for {
      kp      <- cryptoService.generateKeyPair()
      iv      <- cryptoService.randomByte(len = 8)
      pass    <- cryptoService.enforceDes3Key(BytesValue(rand))
      encPriv <- cryptoService.des3cbcEncrypt(kp.priv, pass, iv)
      uuid    <- cryptoService.randomUUID()
      acc     <- accountService.createAccount(kp.publ, encPriv, iv, uuid)
    } yield acc

    // then, save the account as an inactive one
    def saveAccountAsInactive(account: Account): SP[F, Account] =
      for {
        acc <- accountSnapshot.saveInactiveAccount(account)
      } yield acc

    // finally, we disseminate the ACCOUNT-CREATED message to warriors
    //   they will save the account data too
    def disseminateAccountCreated(account: Account): SP[F, Unit] =
      for {
        self       <- networkService.currentNode()
        warriors   <- networkService.warriorNodesOfNymph(self)
        dataPacket <- networkService.buildCreateAccountDataMessage(account)
        _          <- networkService.disseminate(dataPacket, warriors)
      } yield ()

    // combine these steps to describe the complete process.
    for {
      _          <- log.info(s"begin to handle enrollment: $rand")
      t0         <- monitorService.startNow()
      acc0       <- createAccount
      acc1       <- saveAccountAsInactive(acc0)
      _          <- disseminateAccountCreated(acc1)
      timePassed <- monitorService.timePassed(t0)
      _          <- log.info(s"finish handling enrollment: $rand. from $t0, time passed: $timePassed")
    } yield acc1
  }

  /**
    * uc2. query account detail.
    *
    * @param id account id
    * @return if the account id existed in current node or other nodes, return the account detail, or None.
    */
  override def queryAccount(id: Account.ID): SP[F, Option[Account]] = {
    // first, try to load account info locally,
    // if not found return None, then send a ACCOUNT-SYNC message,
    //    async get account data from warrior

    val disseminateAccountSyncMessage: SP[F, Unit] = for {
      self       <- networkService.currentNode()
      warriors   <- networkService.warriorNodesOfNymph(self)
      dataPacket <- networkService.buildSyncAccountMessage(id)
      _          <- networkService.disseminate(dataPacket, warriors)
    } yield ()

    for {
      accOpt <- accountSnapshot.findAccount(id)
      _      <- if (accOpt.isDefined) nothingToDo else disseminateAccountSyncMessage
    } yield accOpt
  }

  /**
    * uc3. send a transaction.
    *
    * @param id          sender's account id
    * @param transaction transaction info
    * @return return the transaction's current status
    */
  override def sendTransaction(id: Account.ID,
                               transaction: Transaction): SP[F, Transaction.Status] = {
    // we should validate transaction:
    // 1. transaction should be signed by the account


    val existedAccount: SP[F, Account] = for {
      accOpt <- queryAccount(id)
      acc <- err.either(
        Either.cond(accOpt.isDefined, accOpt.get, AccountNotFound(id))
      )
    } yield acc

    def transactionWithRightSignature(account: Account): SP[F, Transaction] =
      for {
        passed <- cryptoService.validateSignature(transaction.signature, account.pub)
        trans <- err.either(
          Either.cond(passed, transaction, IllegalSignature("TRANSACTION"))
        )
      } yield trans

    /*
    def affordableTransaction(transaction: Transaction, account: Account): SP[F, Transaction] =
      for {
        cost <- transactionService.instrumentCost(transaction)
        trans <- err.either(
          Either.cond(
            account.balance.ordered >= cost,
            transaction,
            UnAffordableTransaction(account.id, transaction.id)
          ))
      } yield trans
    */

    def disseminateTransaction(account: Account, transaction: Transaction): SP[F, Unit] =
      for {
        dataPacket <- networkService.buildSubmitTransactionMessage(account, transaction)
        self       <- networkService.currentNode()
        warriors   <- networkService.warriorNodesOfNymph(self)
        _          <- networkService.disseminate(dataPacket, warriors)
      } yield ()

    // put it together
    for {
      acc   <- existedAccount
      trans <- transactionWithRightSignature(acc)
      //_     <- affordableTransaction(trans, acc) // unnecessary to instrument costs here.
      _     <- disseminateTransaction(acc, trans)
    } yield Transaction.Status.Pending(trans.id)
  }

  /**
    * uc4. query current status of a transaction.
    *
    * n@param id id of a transaction
    * @return if transaction existed in the chain, return current status, or None.
    */
  override def queryTransactionStatus(id: Transaction.ID): SP[F, Option[Transaction.Status]] = {
    // first we find transaction locally, if not found, send a SYNC-Transaction message to warrior
    val disseminate: SP[F, Unit] = for {
      self       <- networkService.currentNode()
      warriors   <- networkService.warriorNodesOfNymph(self)
      dataPacket <- networkService.buildSyncTransactionMessage()
      _          <- networkService.disseminate(dataPacket, warriors)
    } yield ()
    for {
      transOpt <- transactionStore.findTransaction(id)
      _        <- if (transOpt.isDefined) nothingToDo else disseminate
    } yield transOpt.map(_.status)
  }

  override def randomizeTransactionID(): SP[F, Transaction.ID] =
    transactionService.randomTransactionID()
}

object Nymph {
  def apply[F[_]](implicit M: Model[F]): Nymph[F] = new Nymph[F] {
    override val model: Model[F] = M
  }
}
