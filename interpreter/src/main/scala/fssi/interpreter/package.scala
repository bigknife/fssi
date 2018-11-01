package fssi

import cats.data.Kleisli
import cats.effect.IO

package object interpreter {
  type Stack[A] = Kleisli[IO, Setting, A]

  object Stack {
    def apply[A](a: => A): Stack[A] = Kleisli { _ =>
      IO(a)
    }
    def apply[A](f: Setting => A): Stack[A] = Kleisli { setting =>
      IO { f(setting) }
    }
  }

  object handlers
      extends CryptoHandler.Implicits
      with ContractHandler.Implicits
      with ConsensusHandler.Implicits
      with NetworkHandler.Implicits
      with StoreHandler.Implicits
      with LogHandler.Implicits
      with bigknife.sop.effect.error.ErrorMInstance

  object runner {
    import bigknife.sop._, implicits._
    import ast.blockchain._
    import ast.blockchain.Model._
    import handlers._

    def runStack[A](p: SP[Model.Op, A]): Stack[A]                         = p.interpret[Stack]
    def runIO[A](p: SP[Model.Op, A], setting: Setting): cats.effect.IO[A] = runStack(p)(setting)
    def runIOAttempt[A](p: SP[Model.Op, A],
                        setting: Setting): cats.effect.IO[Either[Throwable, A]] =
      runStack(p)(setting).attempt
  }

  /** json codecs
    */
  object jsonCodecs
      extends types.json.AllTypesJsonCodec
      with trie.TrieCodecs
      with io.circe.generic.AutoDerivation
}
