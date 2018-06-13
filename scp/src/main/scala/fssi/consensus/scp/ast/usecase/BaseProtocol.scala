package fssi.consensus.scp.ast.usecase

import bigknife.sop._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.components.Model
import fssi.consensus.scp.ast.domain.types.Statement._
import fssi.consensus.scp.ast.domain.types._

/**
  * base of nomination protocol and ballot protocol
  * @tparam F
  */
trait BaseProtocol[F[_]] {
  val model: Model[F]

  /**
    * federated accept
    * @param voted voted predict
    * @param accepted accepted predict
    * @param envs envs, all envelopes
    * @return
    */
  def federatedAccept(voted: Statement.Predict,
                      accepted: Statement.Predict,
                      envs: Map[Node.ID, Envelope]): P[F, Boolean]

  /**
    * federated ratify
    * @param voted voted predict
    * @param envs envs, all envelopes
    * @return
    */
  def federatedRatify(voted: Statement.Predict, envs: Map[Node.ID, Envelope]): P[F, Boolean]
}
