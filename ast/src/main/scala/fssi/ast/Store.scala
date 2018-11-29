package fssi.ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.types.biz._
import java.io._

import fssi.contract.lib.{Context, KVStore, TokenQuery}
import fssi.types.base.{UniqueName, WorldState}
import fssi.types.biz.Contract.UserContract
import fssi.types.biz.{Contract => BizContract}
import fssi.types.exception.FSSIException

@sp trait Store[F[_]] {

  /** create store for a chain, include data store, chain configuration store, etc. logically:
    * 1. chain.conf configuration store
    * 2. store/kv/{chainId}_block.db the block chain store
    * 3. store/kv/{chainId}_contract.db the contracts store
    * 4. store/kv/{chainId}_token.db the account token store
    */
  def createChainStore(root: File, chainId: String): P[F, Unit]

  /** initialized an empty chain store, such as creating genesis block .
    */
  def initialize(root: File, chainId: String): P[F, Unit]

  /** load from an exist store
    */
  def load(root: File): P[F, Unit]

  /** unload current resource
    */
  def unload(): P[F, Unit]

  /** self-check, if something insane, throw exceptios
    */
  def check(): P[F, Unit]

  /** load store from disk, then do self-check
    */
  def loadAndCheck(root: File): SP[F, Unit] = {
    for {
      _ <- load(root)
      _ <- check()
    } yield ()
  }

  def getLatestDeterminedBlock(): P[F, Block]

  def getCurrentWorldState(): P[F, WorldState]

  def persistBlock(block: Block): P[F, Unit]

  def loadAccountFromFile(accountFile: File): P[F, Either[FSSIException, Account]]

  def loadSecretKeyFromFile(secretKeyFile: File): P[F, Either[FSSIException, Account.SecretKey]]

  def snapshotTransaction(transaction: Transaction): P[F, Either[FSSIException, Unit]]

  def loadContract(accountId: Account.ID,
                   contractName: UniqueName,
                   version: BizContract.Version): P[F, Option[UserContract]]

  def prepareKVStore(userContract: UserContract): P[F, KVStore]
  def prepareTokenQuery(userContract: UserContract): P[F, TokenQuery]
  def createContextInstance(kvStore: KVStore,
                            tokenQuery: TokenQuery,
                            invoker: Account.ID): P[F, Context]

  def currentHeight(): P[F, BigInt]
}
