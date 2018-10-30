package fssi

import bigknife.sop._
import bigknife.sop.implicits._

package object ast {
  object blockchain extends BlockChain

  type F[A] = blockchain.Model.Op[A]
  type Program[A] = SP[F, A]
  type Effect = Program[Unit]
  object Effect {
    def empty: Effect = ().pureSP[F]
  }
}
