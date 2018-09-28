package fssi
package interpreter

import contract.lib._
import jsonCodecs._
import utils._
import trie._
import types._
import exception._
import implicits._
import ast._
import java.io._
import java.nio.charset.Charset

import scala.collection._
import io.circe.parser._
import io.circe.syntax._
import Bytes.implicits._
import better.files.{File => ScalaFile, _}
import java.io._
import java.nio.file.Paths

import fssi.contract.scaffold.ContractScaffold
import fssi.sandbox.exception.ContractRunningException

class ContractServiceHandler extends ContractService.Handler[Stack] with BlockCalSupport {

  val sandbox = new fssi.sandbox.SandBox
  lazy val scaffold = new ContractScaffold

  /** create contract project
    *
    */
  override def createContractProject(projectRoot: File): Stack[Unit] = Stack { setting =>
    scaffold.createContractProject(projectRoot.toPath).right.get
  }

  /** check current contract running environment
    */
  override def checkRunningEnvironment(): Stack[Either[FSSIException, Unit]] = Stack { setting =>
    sandbox.checkRunningEnvironment
  }

  /** check the smart contract project to see where it is full-deterministic or not
    */
  override def checkDeterminismOfContractProject(
      rootPath: File): Stack[Either[FSSIException, Unit]] = Stack { setting =>
//    sandbox.checkContractDeterminism(rootPath)
    Right(())
  }

  /** compile smart contract project and output to the target file
    */
  override def compileContractProject(accountId: biz.Account.ID,
                                      pubKey: biz.Account.PubKey,
                                      privKey: biz.Account.PrivKey,
                                      rootPath: File,
                                      sandboxVersion: String,
                                      outputFile: File): Stack[Either[FSSIException, Unit]] =
    Stack { setting =>
      sandbox.compileContract(accountId,
                              pubKey,
                              privKey,
                              rootPath.toPath,
                              sandboxVersion,
                              outputFile)
    }

  /** create a running context for some transaction
    */
  override def createContextInstance(sqlStore: SqlStore,
                                     kvStore: KVStore,
                                     tokenQuery: TokenQuery,
                                     currentAccountId: String): Stack[Context] = Stack { setting =>
    ContractRunningContext(sqlStore, kvStore, tokenQuery, currentAccountId)
  }

  override def createUserContractFromContractFile(
      account: Account,
      contractFile: File,
      contractName: UniqueName,
      contractVersion: Version): Stack[Either[FSSIException, Contract.UserContract]] = Stack {
    setting =>
//      sandbox.buildContract(contractFile)
      Left(new FSSIException(""))
  }

  override def invokeUserContract(context: Context,
                                  contract: Contract.UserContract,
                                  method: Contract.Method,
                                  params: Contract.Parameter): Stack[Either[Throwable, Unit]] =
    Stack { setting =>
      contract.meta.methods.find(_.alias == method.alias) match {
        case Some(_) =>
          val contractFile =
            Paths
              .get(System.getProperty("user.home"),
                   s".fssi/.${contract.name.value}_${contract.version.value}")
              .toFile
          if (!contractFile.getParentFile.exists()) contractFile.getParentFile.mkdirs()
          if (contractFile.exists()) FileUtil.deleteDir(contractFile.toPath)
          contractFile.createNewFile()
          val fileOutputStream = new FileOutputStream(contractFile, true)
          try {
            fileOutputStream.write(contract.code.bytes, 0, contract.code.bytes.length)
            fileOutputStream.flush()
//            sandbox.executeContract(context, contractFile, method, params)
            Right(())
          } catch {
            case t: Throwable => Left(t)
          } finally {
            if (fileOutputStream != null) fileOutputStream.close()
            if (contractFile.exists()) FileUtil.deleteDir(contractFile.toPath)
          }
        case None =>
          Left(ContractRunningException(Vector(
            s"can not find method ${method.alias} in contract ${contract.name.value}#${contract.version.value}")))
      }
    }

  override def measureCostToPublishContract(
      publishContract: Transaction.PublishContract): Stack[Token] =
    Stack {
      Token(amount = BigInt(0), tokenUnit = Token.Unit.Sweet)
    }

  override def measureCostToRunContract(contract: Contract.UserContract): Stack[Token] =
    Stack {
      Token(amount = BigInt(0), tokenUnit = Token.Unit.Sweet)
    }

  override def measureCostToTransfer(transferedToken: Token): Stack[Token] =
    Stack {
      Token(amount = BigInt(0), tokenUnit = Token.Unit.Sweet)
    }

  override def getContractGlobalIdentifiedName(contract: Contract.UserContract): Stack[String] =
    Stack {
      s"${contract.name.value}#${contract.version.value}"
    }

  /** calculate bytes of user contract for beeing singed
    */
  override def calculateSingedBytesOfUserContract(
      userContract: Contract.UserContract): Stack[BytesValue] = Stack { setting =>
    calculateBytesToBeSignedOfUserContract(userContract)
  }
}

object ContractServiceHandler {
  val instance = new ContractServiceHandler

  trait Implicits {
    implicit val contractServiceHandler: ContractServiceHandler = instance
  }
}
