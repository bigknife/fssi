package fssi.ast.domain

import fssi.ast.domain.types._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

@sp trait LedgerService[F[_]] {

  /**
    * create time capsule
    * @param height height
    * @param previousHash previous hash
    * @param moments current moments
    * @return
    */
  def createTimeCapsule(height: BigInt,
                        previousHash: Hash,
                        moments: Vector[Moment]): P[F, TimeCapsule]

  /**
    * create genius block
    * @return
    */
  def createGeniusTimeCapsule(): P[F, TimeCapsule]
}
