package fssi.ast.usecase

import bigknife.sop._
import implicits._
import fssi.ast.domain.components.Model
import fssi.ast.domain.types.Contract.Parameter
import fssi.ast.domain.types._

trait Warrior[F[_]] extends WarriorUseCases[F] with P2P[F] {

  val model: Model[F]
  import model._

  /**
    * uc1. handle message from Nymph
    *     transaction -> contract -> moment
    */
  override def processTransaction(transaction: Transaction): SP[F, Transaction.Status] = {

    // first, find the account of the transaction's sender
    def findAccount(next: Account => SP[F, Transaction.Status]): SP[F, Transaction.Status] =
      for {
        accOpt <- accountSnapshot.findAccountSnapshot(transaction.sender)
        status <- if (accOpt.isEmpty) for {
          _ <- log.warn(
            s"Account(id=${transaction.sender}) Not Found, Transaction(id=${transaction.id}) rejected!")
          s1 <- Transaction.Status.Rejected(transaction.id).pureSP
        } yield s1
        else next(accOpt.map(_.account).get)
      } yield status

    // then, validate the signature of the transaction
    def validateSignature(account: Account)(
        next: => SP[F, Transaction.Status]): SP[F, Transaction.Status] =
      for {
        publ <- cryptoService.rebuildPubl(account.publicKeyData)
        passed <- cryptoService.validateSignature(transaction.signature,
                                                  transaction.toBeVerified,
                                                  publ)
        status <- if (passed) next
        else
          for {
            _  <- log.warn(s"Transaction(id=${transaction.id})'s signature is illegal")
            s0 <- Transaction.Status.Rejected(transaction.id).pureSP
          } yield s0
      } yield status

    // then, find appropriately contract
    def findProperContract(
        next: (
            Contract,
            Option[Contract.Function],
            Option[Contract.Parameter]) => SP[F, Transaction.Status]): SP[F, Transaction.Status] =
      for {
        contract_name_version_fun_param <- contractService.resolveTransaction(transaction)
        contractOpt <- contractStore.findContract(contract_name_version_fun_param._1,
                                                  contract_name_version_fun_param._2)
        status <- if (contractOpt.isDefined)
          next(contractOpt.get,
               contract_name_version_fun_param._3,
               contract_name_version_fun_param._4)
        else
          for {
            _  <- log.warn(s"Can't Resolve Contract from Transaction(id=${transaction.id})")
            s0 <- Transaction.Status.Rejected(transaction.id).pureSP
          } yield s0
      } yield status

    // then, run the contract
    def runContract(invoker: Account,
                    contract: Contract,
                    function: Option[Contract.Function],
                    parameter: Option[Parameter])(
        next: Moment => SP[F, Transaction.Status]): SP[F, Transaction.Status] =
      for {
        currentStatesOr <- ledgerStore.loadStates(invoker.id, contract, parameter)
        currentStates   <- err.either(currentStatesOr)
        momentOrThrowable <- contractService.runContract(invoker,
                                                         contract,
                                                         function,
                                                         currentStates,
                                                         parameter,
                                                         transaction.id)
        status <- momentOrThrowable match {
          case Left(t) =>
            for {
              _  <- log.error("Contract Run Failed.", Some(t))
              s0 <- Transaction.Status.Failed(transaction.id).pureSP
            } yield s0
          case Right(moment) => next(moment)
        }
      } yield status

    // then, put the moment into the proposal moment pool (contract engine will run proposal consensus periodically
    def putToPool(moment: Moment): SP[F, Transaction.Status] =
      for {
        pooled <- consensusEngine.poolMoment(moment)
        status <- (if (pooled) Transaction.Status.Pending(transaction.id)
                   else Transaction.Status.Rejected(transaction.id)).pureSP
        // fire to run consensus to validate proposal
        _ <- validateProposal()
      } yield status

    // put  together
    findAccount { account =>
      validateSignature(account) {
        findProperContract { (contract, function, param) =>
          runContract(account, contract, function, param) { moment =>
            putToPool(moment)
          }
        }
      }
    }
  }

  /**
    * uc2. run consensus when the proposal pool is full or time is up.
    */
  override def validateProposal(): SP[F, Unit] = {
    // first build proposal from moment pool
    def buildProposal(next: Proposal => SP[F, Unit]): SP[F, Unit] =
      for {
        proposalOpt <- consensusEngine.buildProposal()
        _           <- if (proposalOpt.isDefined) next(proposalOpt.get) else ().pureSP[F]
      } yield ()

    // run consensus to validate proposal
    def validate(proposal: Proposal)(next: Proposal => SP[F, Unit]): SP[F, Unit] =
      for {
        validProposal <- consensusEngine.runConsensus(proposal)
        _             <- next(validProposal)
      } yield ()

    // commit agreed proposal
    def commit(proposal: Proposal): SP[F, Unit] =
      for {
        _ <- ledgerStore.commit(proposal)
        _ <- accountSnapshot.commit(proposal)
      } yield ()

    // put together
    buildProposal { proposal =>
      validate(proposal) { validProposal =>
        commit(validProposal)
      }
    }
  }

  /**
    * uc3. handle the message of CreateAccount
    *
    * @param account account created in Nymph, or heard from other warriors
    * @return
    */
  override def createNewAccount(account: Account): SP[F, Unit] =
    for {
      t0       <- monitorService.startNow()
      _        <- log.info(s"creating new account: $account at $t0")
      snapshot <- accountService.makeSnapshot(account)
      _        <- log.info(s"prepared account snapshot: $snapshot")
      _        <- accountSnapshot.saveSnapshot(snapshot)
      passed   <- monitorService.timePassed(t0)
      _ <- log.info(
        s"saved account snapshot, process of creating new account finished, timepassed $passed")
    } yield ()
}

object Warrior {
  def apply[F[_]](implicit M: Model[F]): Warrior[F] = new Warrior[F] {
    override val model: Model[F] = M
  }
}
