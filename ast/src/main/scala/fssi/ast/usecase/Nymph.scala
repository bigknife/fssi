package fssi.ast.usecase

import bigknife.sop._, implicits._
import implicits._
import fssi.ast.domain.components.Model
import fssi.ast.domain.exceptions._
import fssi.ast.domain.types._

/** implementation of core use cases of NymphUseCases
  *
  */
trait Nymph[F[_]] extends NymphUseCases[F] with P2P[F] {
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
      kp          <- cryptoService.generateKeyPair()
      privData    <- cryptoService.privateKeyData(kp.priv)
      publData    <- cryptoService.publicKeyData(kp.publ)
      iv          <- cryptoService.randomByte(len = 8)
      pass        <- cryptoService.enforceDes3Key(BytesValue(rand))
      encPrivData <- cryptoService.des3cbcEncrypt(privData, pass, iv)
      uuid        <- cryptoService.randomUUID()
      acc         <- accountService.createAccount(publData, encPrivData, iv, uuid)
    } yield acc

    // then, save the account as an inactive one
    def saveAccountSnapshot(account: Account): SP[F, Unit] =
      for {
        desensitized <- accountService.desensitize(account)
        snapshot     <- accountService.makeSnapshot(desensitized)
        _            <- accountSnapshot.saveSnapshot(snapshot)
      } yield ()

    // finally, we disseminate the ACCOUNT-CREATED message to warriors
    //   they will save the account data too
    def disseminateAccountCreated(account: Account): SP[F, Unit] =
      for {
        self       <- networkStore.currentNode()
        warriors   <- networkService.warriorNodesOfNymph(self.get)
        dataPacket <- networkService.buildCreateAccountDataMessage(account)
        _          <- networkService.disseminate(dataPacket, warriors)
      } yield ()

    // combine these steps to describe the complete process.
    for {
      t0         <- monitorService.startNow()
      _          <- log.info(s"begin to handle registration at $t0 with rand($rand)")
      acc        <- createAccount
      _          <- log.info(s"created account(${acc.id.value})")
      _          <- saveAccountSnapshot(acc)
      _          <- log.info(s"saved snapshot of account(${acc.id.value})")
      _          <- disseminateAccountCreated(acc)
      _          <- log.info(s"disseminated account(${acc.id.value})")
      timePassed <- monitorService.timePassed(t0)
      _          <- log.info(s"finish handling registration with rand($rand) within $timePassed ms")
    } yield acc
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
      self       <- networkStore.currentNode()
      warriors   <- networkService.warriorNodesOfNymph(self.get)
      dataPacket <- networkService.buildSyncAccountMessage(id)
      _          <- networkService.disseminate(dataPacket, warriors)
    } yield ()

    for {
      accOpt <- accountSnapshot.findAccountSnapshot(id)
      _      <- if (accOpt.isDefined) nothingToDo else disseminateAccountSyncMessage
    } yield accOpt.map(_.account)
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
        publ <- cryptoService.rebuildPubl(account.publicKeyData)
        passed <- cryptoService.validateSignature(transaction.signature,
                                                  transaction.toBeVerified,
                                                  publ)
        trans <- err.either(
          Either.cond(passed, transaction, IllegalSignature("TRANSACTION"))
        )
      } yield trans

    def disseminateTransaction(account: Account, transaction: Transaction): SP[F, Unit] =
      for {
        dataPacket <- networkService.buildSubmitTransactionMessage(account, transaction)
        self       <- networkStore.currentNode()
        warriors   <- networkService.warriorNodesOfNymph(self.get)
        _          <- networkService.disseminate(dataPacket, warriors)
      } yield ()

    // put it together
    for {
      acc   <- existedAccount
      trans <- transactionWithRightSignature(acc)
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
      self       <- networkStore.currentNode()
      warriors   <- networkService.warriorNodesOfNymph(self.get)
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
