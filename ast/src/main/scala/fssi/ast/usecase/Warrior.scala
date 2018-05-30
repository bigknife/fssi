package fssi.ast.usecase

import bigknife.sop._
import implicits._
import fssi.ast.domain.{Moment, Proposal}
import fssi.ast.domain.components.Model
import fssi.ast.domain.types.{Account, Contract, Signature, Transaction}

trait Warrior[F[_]] extends WarriorUseCases[F] {

  val model: Model[F]
  import model._

  /**
    * uc1. handle message from Nymph
    */
  override def processTransaction(transaction: Transaction): SP[F, Transaction.Status] = {

    // first, find the account of the transaction's sender
    def findAccount(next: Account => SP[F, Transaction.Status]): SP[F, Transaction.Status] =
      for {
        accOpt <- accountSnapshot.findAccount(transaction.sender)
        status <- if (accOpt.isEmpty) for {
          _ <- log.warn(
            s"Account(id=${transaction.sender}) Not Found, Transaction(id=${transaction.id}) rejected!")
          s1 <- Transaction.Status.Rejected(transaction.id).pureSP
        } yield s1
        else next(accOpt.get)
      } yield status

    // then, validate the signature of the transaction
    def validateSignature(account: Account)(
        next: => SP[F, Transaction.Status]): SP[F, Transaction.Status] =
      for {
        passed <- cryptoService.validateSignature(transaction.signature, account.pub)
        status <- if (passed) next
        else
          for {
            _  <- log.warn(s"Transaction(id=${transaction.id})'s signature is illegal")
            s0 <- Transaction.Status.Rejected(transaction.id).pureSP
          } yield s0
      } yield status

    // then, find appropriately contract
    def findProperContract(next: Contract => SP[F, Transaction.Status]): SP[F, Transaction.Status] =
      for {
        contract_name_version <- contractService.resolveTransaction(transaction)
        contractOpt <- contractStore.findContract(contract_name_version._1,
                                                  contract_name_version._2)
        status <- if (contractOpt.isDefined) next(contractOpt.get)
        else
          for {
            _  <- log.warn(s"Can't Resolve Contract from Transaction(id=${transaction.id})")
            s0 <- Transaction.Status.Rejected(transaction.id).pureSP
          } yield s0
      } yield status

    // then, run the contract
    def runContract(invoker: Account, contract: Contract)(
        next: Moment => SP[F, Transaction.Status]): SP[F, Transaction.Status] =
      for {
        momentOrThrowable <- contractService.runContract(invoker, contract)
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
        _      <- consensusEngine.poolMoment(moment)
        status <- Transaction.Status.Pending(transaction.id).pureSP
      } yield status

    // put it together
    findAccount { account =>
      validateSignature(account) {
        findProperContract { contract =>
          runContract(account, contract) { moment =>
            putToPool(moment)
          }
        }
      }
    }
  }
}
