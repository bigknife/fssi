package fssi
package interpreter
import java.io.File

import fssi.ast.Store
import fssi.types.biz.{Account, Block, ChainConfiguration}
import fssi.types.exception.FSSIException
import io.circe._
import io.circe.parser._
import fssi.types.json.implicits._
import io.circe.generic.auto._
import better.files._
import fssi.types.base.BytesValue

import scala.util.Try

class StoreHandler extends Store.Handler[Stack] {

  /** create store for a chain, include data store, chain configuration store, etc. logically:
    * 1. chain.conf configuration store
    * 2. store/kv/{chainId}_block.db the block chain store
    * 3. store/kv/{chainId}_contract.db the contracts store
    * 4. store/kv/{chainId}_token.db the account token store
    */
  override def createChainStore(root: File, chainId: String): Stack[Unit] = Stack {

  }

  /** initialized an empty chain store, such as creating genesis block .
    */
  override def initialize(root: File, chainId: String): Stack[Unit] = ???

  /** load from an exist store
    */
  override def load(root: File): Stack[Unit] = ???

  /** unload current resource
    */
  override def unload(): Stack[Unit] = ???

  /** self-check, if something insane, throw exceptios
    */
  override def check(): Stack[Unit] = ???

  /** get conf store
    */
  override def getChainConfiguration(): Stack[ChainConfiguration] = ???

  override def getLatestDeterminedBlock(): Stack[Block] = ???

  override def persistBlock(block: Block): Stack[Unit] = ???

  override def loadAccountFromFile(accountFile: File): Stack[Either[FSSIException, Account]] =
    Stack {
      Try {
        val accountJsonString = accountFile.toScala.contentAsString
        val result = for {
          json    <- parse(accountJsonString)
          account <- json.as[Account]
        } yield account
        result.right.get
      }.toEither.left.map(e =>
        new FSSIException(s"load account from file ($accountFile) failed", Option(e)))
    }

  override def loadSecretKeyFromFile(
      secretKeyFile: File): Stack[Either[FSSIException, Account.SecretKey]] = Stack {
    Try {
      val secretKeyString = secretKeyFile.toScala.contentAsString
      val secretBytes     = BytesValue.unsafeDecodeBcBase58(secretKeyString).bytes
      Account.SecretKey(secretBytes)
    }.toEither.left.map(e =>
      new FSSIException(s"load secret key from file $secretKeyFile failed", Option(e)))
  }
}

object StoreHandler {
  val instance = new StoreHandler

  trait Implicits {
    implicit val storeHandler: StoreHandler = instance
  }
}
