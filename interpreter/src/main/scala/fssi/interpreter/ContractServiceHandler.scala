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

  val compiler = new fssi.sandbox.Compiler

  /** check the smart contract project to see where it is full-deterministic or not
    */
  override def checkDeterminismOfContractProject(
      rootPath: File): Stack[Either[FSSIException, Unit]] = Stack { setting =>
    compiler.checkDeterminism(rootPath)
  }

  /** compile smart contract project and output to the target file
    */
  override def compileContractProject(rootPath: File,
                                      sandboxVersion: String,
                                      outputFile: File): Stack[Either[FSSIException, Unit]] =
    Stack { setting =>
      compiler.compileContract(rootPath.toPath, sandboxVersion, outputFile)
    }

  /** create a running context for some transaction
    */
  override def createContextInstance(sqlStore: SqlStore,
                                     kvStore: KVStore,
                                     tokenQuery: TokenQuery): Stack[Context] = Stack { setting =>
    ContractRunningContext(sqlStore, kvStore, tokenQuery)
  }
}

object ContractServiceHandler {
  val instance = new ContractServiceHandler

  trait Implicits {
    implicit val contractServiceHandler: ContractServiceHandler = instance
  }
}
