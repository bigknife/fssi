package fssi
package interpreter
import java.io.File
import java.util.UUID

import fssi.ast.Contract
import fssi.contract.lib.Context
import fssi.types.ApplicationMessage
import fssi.types.base.{Signature, UniqueName}
import fssi.types.biz.Contract.{UserContract, Version}
import fssi.types.biz.Message.ApplicationMessage.TransactionMessage
import fssi.types.biz.Message.ClientMessage.SendTransaction
import fssi.types.biz._
import fssi.types.exception.FSSIException
import fssi.types.implicits._
import io.circe._
import io.circe.parser._
import fssi.types.json.implicits._

class ContractHandler extends Contract.Handler[Stack] {

  val sandbox = new fssi.sandbox.SandBox

  val scaffold = new fssi.contract.scaffold.ContractScaffold

  /** check runtime, if not acceptable, throw exception
    */
  override def assertRuntime(): Stack[Unit] = Stack {
    sandbox.checkRunningEnvironment match {
      case Right(_) => ()
      case Left(e)  => throw e
    }
  }

  override def initializeRuntime(): Stack[Unit] = Stack {}

  override def closeRuntime(): Stack[Unit] = Stack {}

  override def createContractProject(projectRoot: File): Stack[Either[FSSIException, Unit]] =
    Stack {
      scaffold.createContractProject(projectRoot.toPath)
    }

  override def compileContractProject(accountId: Account.ID,
                                      pubKey: Account.PubKey,
                                      privateKey: Account.PrivKey,
                                      projectDir: File,
                                      sandboxVersion: String,
                                      output: File): Stack[Either[FSSIException, Unit]] = Stack {
    sandbox.compileContract(accountId,
                            pubKey,
                            privateKey,
                            projectDir.toPath,
                            sandboxVersion,
                            output)
  }

  override def checkContractDeterminism(pubKey: Account.PubKey,
                                        contractFile: File): Stack[Either[FSSIException, Unit]] =
    Stack {
      sandbox.checkContractDeterminism(pubKey, contractFile)
    }

  override def invokeContract(
      context: Context,
      contractCode: UserContract.Code,
      method: UserContract.Method,
      params: Option[UserContract.Parameter]): Stack[Either[FSSIException, Unit]] =
    Stack {
      sandbox.executeContract(context, contractCode, method, params)
    }

  override def loadContractFromFile(
      pubKey: Account.PubKey,
      contractFile: File): Stack[Either[FSSIException, UserContract]] = Stack {
    sandbox.buildUnsignedContract(pubKey, contractFile)
  }

  override def generateTransactionID(): Stack[Transaction.ID] = Stack {
    val uuid = UUID.randomUUID().toString.replace("-", "")
    Transaction.ID(uuid.getBytes("utf-8"))
  }

  override def createTransferTransaction(transactionId: Transaction.ID,
                                         payer: Account.ID,
                                         publicKey: Account.PubKey,
                                         payee: Account.ID,
                                         token: Token): Stack[Transaction.Transfer] =
    Stack {
      Transaction.Transfer(transactionId,
                           payer,
                           publicKey,
                           payee,
                           token,
                           Signature.empty,
                           System.currentTimeMillis())
    }

  override def createDeployTransaction(transactionId: Transaction.ID,
                                       owner: Account.ID,
                                       publicKey: Account.PubKey,
                                       contract: UserContract): Stack[Transaction.Deploy] =
    Stack {
      Transaction.Deploy(transactionId,
                         owner,
                         publicKey,
                         contract,
                         Signature.empty,
                         System.currentTimeMillis())
    }

  override def createRunTransaction(
      transactionId: Transaction.ID,
      caller: Account.ID,
      publicKey: Account.PubKey,
      owner: Account.ID,
      contractName: UniqueName,
      contractVersion: Version,
      methodAlias: String,
      contractParameter: Option[UserContract.Parameter]): Stack[Transaction.Run] = Stack {
    Transaction.Run(transactionId,
                    caller,
                    publicKey,
                    owner,
                    contractName,
                    contractVersion,
                    methodAlias,
                    contractParameter,
                    Signature.empty,
                    System.currentTimeMillis())
  }

  override def transferMessageToTransaction(message: Message): Stack[Option[Transaction]] = Stack {
    val bytesArray = message match {
      case transactionMessage: TransactionMessage => transactionMessage.payload
      case sendTransaction: SendTransaction       => sendTransaction.payload
      case _                                      => Array.emptyByteArray
    }
    if (bytesArray.isEmpty) None
    else parse(bytesArray.asBytesValue.utf8String).right.get.as[Transaction].toOption
  }
}

object ContractHandler {
  val instance = new ContractHandler

  trait Implicits {
    implicit val contractHandler: ContractHandler = instance
  }
}
