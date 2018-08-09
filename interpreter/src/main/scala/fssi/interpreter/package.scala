package fssi

import utils.trie._

import cats.data.Kleisli
import cats.effect.IO
import java.io._

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
      with NetworkHandler.Implicits
      with BlockServiceHandler.Implicits
      with BlockStoreHandler.Implicits
      with TokenStoreHandler.Implicits
      with ContractStoreHandler.Implicits
      with ContractDataStoreHandler.Implicits
      with bigknife.sop.effect.error.ErrorMInstance

  object runner {
    import bigknife.sop._, implicits._
    import ast.components._
    import ast.components.Model._
    import handlers._

    def runStack[A](p: SP[Model.Op, A]): Stack[A]                         = p.interpret[Stack]
    def runIO[A](p: SP[Model.Op, A], setting: Setting): cats.effect.IO[A] = runStack(p)(setting)
    def runIOAttempt[A](p: SP[Model.Op, A],
                        setting: Setting): cats.effect.IO[Either[Throwable, A]] =
      runStack(p)(setting).attempt
  }

  /** a store based leveldb, used for utils.trie
    */
  object levelDBStore {
    def apply(path: String): Store = new LevelDBStore {
      override val dbFile: File = {
        val f = new java.io.File(path)
        if (!f.exists()) {
          f.mkdirs()
        }
        else require(f.isDirectory)
        f
      }
    }
  }
}
