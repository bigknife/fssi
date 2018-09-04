package fssi
package interpreter

import contract.lib._
import jsonCodecs._
import utils._
import trie._
import types._, exception._
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

class ContractServiceHandler extends ContractService.Handler[Stack] {

  /** check the smart contract project to see where it is full-deterministic or not
    */
  override def checkDeterminismOfContractProject(
      rootPath: File): Stack[Either[FSSIException, Unit]] = Stack { setting =>
    ???
  }

  /** compile smart contract project and output to the target file
    */
  override def compileContractProject(rootPath: File,
                                      sandboxVersion: String,
                                      outputFile: File): Stack[Either[FSSIException, Unit]] =
    Stack { setting =>
      ???
    }

  /** resolve contract gid
    */
  override def resolveContractGlobalIdentifiedName(name: UniqueName,
                                                   version: Version): Stack[String] = Stack {
    setting =>
      s"${name.value}#${version.value}"
  }

  /** create context for a transaction in current block (height)
    */
  override def createContractRunningContextInstance(
      height: BigInt,
      transaction: Transaction.RunContract): Stack[Context] = Stack { setting =>
    new Context {
      /**
        * return a running-time specified kv store
        */
      override def kvStore(): KVStore = ???

      /**
        * return a running-time specified sql store
        */
      override def sqlStore(): SqlStore = ???

      /**
        * return a running-time specified token querier.
        */
      override def tokenQuery(): TokenQuery = ???
    }
  }

  /** run smart contract
    *
    */
  override def invokeContract(contract: Contract.UserContract,
                              method: Contract.Method,
                              param: Contract.Parameter,
                              context: Context): Stack[Either[Throwable, Unit]] = Stack { setting =>
    ???
  }
}

object ContractServiceHandler {
  val instance = new ContractServiceHandler

  trait Implicits {
    implicit val contractServiceHandler: ContractServiceHandler = instance
  }
}
