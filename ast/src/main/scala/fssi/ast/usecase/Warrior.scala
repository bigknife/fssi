package fssi.ast.usecase

import bigknife.sop._
import fssi.ast.domain.Node
import implicits._
import fssi.ast.domain.components.Model
import fssi.ast.domain.types.Contract.Parameter
import fssi.ast.domain.types._
import fssi.contract.States

trait Warrior[F[_]] extends WarriorUseCases[F] with P2P[F] {

  val model: Model[F]
  import model._

  /** start a p2p node, connect to some seed nodes
    * if there are no seed nodes, start this node as a seed.
    */
  override def startup(accountPublicKey: String,
                       ip: String,
                       port: Int,
                       seeds: Vector[String],
                       handler: DataPacket => Unit): SP[F, Node] = {
    val p2pStartup = super.startup(accountPublicKey, ip, port, seeds, handler)
    // if ledger is empty, try to create first time capsule (genius block)
    val tryCreateFirstTimeCapusle: SP[F, Unit] = {
      for {
        ch    <- ledgerStore.currentHeight()
        tcOpt <- ledgerStore.findTimeCapsuleAt(ch)
        _ <- if (tcOpt.isDefined) ().pureSP[F]
        else {
          // create genius block
          for {
            genius <- ledgerService.createGeniusTimeCapsule()
            _      <- ledgerStore.saveTimeCapsule(genius)
          } yield ()
        }
      } yield ()
    }

    for {
      node <- p2pStartup
      _    <- tryCreateFirstTimeCapusle
    } yield node
  }


  /**
    * run a transaction temporally, update States of same key from lastStates.
    *
    * @param transaction transaction
    * @return states jump, from old states to new states
    */
  override def runTransaction(transaction: Transaction, lastStates: States): SP[F, Option[Moment]] = {
    def findAccount(next: Node => SP[F, Option[Moment]]): SP[F, Option[Moment]] =
      for {
        nodeOpt <- networkStore.currentNode()
        status <- if (/*accOpt.isEmpty || */nodeOpt.isEmpty) for {
          _ <- log.warn(
            s"current node Not Found, " +
              s"runTransaction(id=${transaction.id}) failed!")
          s1 <- None.pureSP
        } yield s1
        else
          for {
            _ <- log.info(s"running transaction on ${nodeOpt.get}")
            x <- next(nodeOpt.get)
          } yield x

      } yield status

    def validateSignature(next: => SP[F, Option[Moment]]): SP[F, Option[Moment]] =
      for {
        publ <- cryptoService.rebuildPubl(BytesValue.decodeHex(transaction.sender.value))
        passed <- cryptoService.validateSignature(transaction.signature,
          transaction.toBeVerified,
          publ)
        status <- if (passed) next
        else
          for {
            _  <- log.warn(s"runTransaction(id=${transaction.id})'s signature is illegal")
            s0 <- None.pureSP
          } yield s0
      } yield status

    def findProperContract(
                            next: (
                              Contract,
                                Option[Contract.Function],
                                Option[Contract.Parameter]) => SP[F, Option[Moment]]): SP[F, Option[Moment]] =
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
            _  <- log.warn(s"Can't Find Contract Invoked In Transaction(id=${transaction.id})")
            s0 <- None.pureSP
          } yield s0
      } yield status

    def runContract(contract: Contract,
                    function: Option[Contract.Function],
                    parameter: Option[Parameter])(
                     next: Moment => SP[F, Option[Moment]]): SP[F, Option[Moment]] =
      for {
        currentStatesOr <- ledgerStore.loadStates(transaction.sender, contract, parameter)
        currentStates   <- err.either(currentStatesOr)
        statesChangeOrThrowable <- contractService.runContract(transaction.sender,
          contract,
          function,
          currentStates.updateStates(lastStates), // update last states
          parameter)
        status <- statesChangeOrThrowable match {
          case Left(t) =>
            for {
              _  <- log.error(s"Contract Run Failed. ${t.getMessage}", Some(t))
              s0 <- None.pureSP
            } yield s0
          case Right(statesChange) =>
            for {
              prevBytes <- transactionService.calculateStatesToBeSigned(statesChange.previous)
              prevSign  <- cryptoService.hash(prevBytes)
              currBytes <- transactionService.calculateStatesToBeSigned(statesChange.current)
              currSign  <- cryptoService.hash(currBytes)
              moment <- transactionService.createMoment(transaction,
                statesChange,
                prevSign,
                currSign)
              x <- next(moment)
            } yield x
        }
      } yield status


    findAccount { node =>
      validateSignature {
        findProperContract { (contract, function, param) =>
          runContract(contract, function, param) { moment =>
            Option(moment).pureSP[F]
          }
        }
      }
    }
  }

  /**
    * uc1. handle message from Nymph
    *     transaction -> contract -> moment
    */
  override def processTransaction(transaction: Transaction): SP[F, Transaction.Status] = {

    // first, find the account of the transaction's sender
    def findAccount(next: Node => SP[F, Transaction.Status]): SP[F, Transaction.Status] =
      for {
        accOpt  <- accountSnapshot.findAccountSnapshot(transaction.sender)
        nodeOpt <- networkStore.currentNode()
        status <- if (/*accOpt.isEmpty || */nodeOpt.isEmpty) for {
          _ <- log.warn(
            s"current node Not Found, " +
              s"Transaction(id=${transaction.id}) rejected!")
          s1 <- Transaction.Status.Rejected(transaction.id).pureSP
        } yield s1
        else
          for {
            _ <- log.info(s"processing transaction on ${nodeOpt.get}")
            x <- next(nodeOpt.get)
          } yield x

      } yield status

    // then, validate the signature of the transaction
    def validateSignature(next: => SP[F, Transaction.Status]): SP[F, Transaction.Status] =
      for {
        publ <- cryptoService.rebuildPubl(BytesValue.decodeHex(transaction.sender.value))
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
            _  <- log.warn(s"Can't Find Contract Invoked In Transaction(id=${transaction.id})")
            s0 <- Transaction.Status.Rejected(transaction.id).pureSP
          } yield s0
      } yield status

    // then, run the contract
    def runContract(contract: Contract,
                    function: Option[Contract.Function],
                    parameter: Option[Parameter])(
        next: Moment => SP[F, Transaction.Status]): SP[F, Transaction.Status] =
      for {
        currentStatesOr <- ledgerStore.loadStates(transaction.sender, contract, parameter)
        currentStates   <- err.either(currentStatesOr)
        statesChangeOrThrowable <- contractService.runContract(transaction.sender,
                                                               contract,
                                                               function,
                                                               currentStates,
                                                               parameter)
        status <- statesChangeOrThrowable match {
          case Left(t) =>
            for {
              _  <- log.error(s"Contract Run Failed. ${t.getMessage}", Some(t))
              s0 <- Transaction.Status.Failed(transaction.id).pureSP
            } yield s0
          case Right(statesChange) =>
            for {
              prevBytes <- transactionService.calculateStatesToBeSigned(statesChange.previous)
              prevSign  <- cryptoService.hash(prevBytes)
              currBytes <- transactionService.calculateStatesToBeSigned(statesChange.current)
              currSign  <- cryptoService.hash(currBytes)
              moment <- transactionService.createMoment(transaction,
                                                        statesChange,
                                                        prevSign,
                                                        currSign)
              x <- next(moment)
            } yield x
        }
      } yield status

    // then, put the moment into the proposal moment pool (contract engine will run proposal consensus periodically
    def putToPool(node: Node, moment: Moment): SP[F, Transaction.Status] =
      for {
        currentHeight      <- ledgerStore.currentHeight()
        currentTimeCapsule <- ledgerStore.timeCapsuleOf(currentHeight)
        pooled <- consensusEngine.poolMoment(node,
                                             currentHeight,
                                             currentTimeCapsule.moments,
                                             moment)
        _ <- log.info(s"$moment pooling: $pooled")
        status <- (if (pooled) Transaction.Status.Pending(transaction.id)
                   else Transaction.Status.Rejected(transaction.id)).pureSP
      } yield status

    // put  together
    //todo: find account can be used to validate the transaction'sender has enough token to run the contract
    //      but now, ignore it.
    findAccount { node =>
      validateSignature {
        findProperContract { (contract, function, param) =>
          runContract(contract, function, param) { moment =>
            putToPool(node, moment)
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

  /**
    * use account's public key to sign a data block
    *
    * @param bytes          data
    * @param publicKeyData account's public key
    * @return
    */
  override def signData(bytes: Array[Byte], publicKeyData: BytesValue): SP[F, BytesValue] = {
    for {
      currentNode <- networkStore.currentNode()
      priv  <- cryptoService.rebuildPriv(currentNode.get.accountPrivateKey)
      sign  <- cryptoService.makeSignature(BytesValue(bytes), priv)
    } yield sign
  }

  /**
    * broadcast message to peers
    * @param message message
    * @return
    */
  override def broadcastMessage(message: DataPacket): SP[F, Unit] =
    for {
      _ <- log.debug(s"start to broadcast message: $message")
      _ <- networkService.broadcast(message)
    } yield ()

  /**
    * query current warrior node info
    * @return
    */
  override def currentNode(): SP[F, Node] = {
    for {
      nodeOpt <- networkStore.currentNode()
    } yield nodeOpt.get
  }

  /**
    * verify data's sign
    *
    * @param source         source bytes
    * @param publicKeyData public key
    * @return
    */
  override def verifySign(source: Array[Byte],
                          signature: Array[Byte],
                          publicKeyData: Array[Byte]): SP[F, Boolean] =
    for {
      priv <- cryptoService.rebuildPubl(BytesValue(publicKeyData))
      ret  <- cryptoService.validateSignature(Signature(signature), BytesValue(source), priv)
    } yield ret

  /**
    * after consensus engine run, moments have reached agreement, so we can persist them
    *
    * @param height  the new block length to be persisted
    * @param moments all moments
    * @return
    */
  override def momentsDetermined(moments: Vector[Moment], height: BigInt): SP[F, Unit] = {
    val saveMoments: SP[F, Unit] = moments.foldLeft(().pureSP[F]) { (acc, n) =>
      for {
        _ <- ledgerStore.saveStates(n.newStates.states)
      } yield ()
    }

    for {
      currentHeight      <- ledgerStore.currentHeight()
      currentTimeCapsule <- ledgerStore.timeCapsuleOf(currentHeight)
      timeCapsule <- ledgerService.createTimeCapsule(currentHeight + 1,
                                                     currentTimeCapsule.hash,
                                                     moments)
      _ <- ledgerStore.saveTimeCapsule(timeCapsule)
      _ <- ledgerStore.updateHeight(currentHeight + 1)
      _ <- saveMoments
    } yield ()
  }
}

object Warrior {
  def apply[F[_]](implicit M: Model[F]): Warrior[F] = new Warrior[F] {
    override val model: Model[F] = M
  }
}
