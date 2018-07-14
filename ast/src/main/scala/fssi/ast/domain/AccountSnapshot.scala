package fssi.ast.domain

import bigknife.sop._, macros._, implicits._
import fssi.ast.domain.types._

/** account store */
@sp trait AccountSnapshot[F[_]] {

  /** startup snapshot db */
  def startupSnapshotDB(): P[F, Unit]

  /** close snapshot db */
  def shutdownSnapshotDB(): P[F, Unit]

  /** save an account which is not active */
  def saveSnapshot(snapshot: Account.Snapshot): P[F, Account.Snapshot]

  /** find account with an id */
  def findAccountSnapshot(id: Account.ID): P[F, Option[Account.Snapshot]]

  /**
    * find account by public key
    * @param publicKey public key data
    * @return
    */
  def findByPublicKey(publicKey: BytesValue): P[F, Option[Account.Snapshot]]

  /** todo: commit new state(moments in proposal) */
  def commit(proposal: Proposal): P[F, Unit]
}
