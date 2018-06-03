package fssi

import cats.data.Kleisli
import cats.effect.IO
import fssi.interpreter.codec._

package object interpreter {
  type Stack[A] = Kleisli[IO, Setting, A]

  object Stack {
    def apply[A](a: => A): Stack[A] = Kleisli { _ =>
      IO(a)
    }
    def apply[A](f: Setting => A): Stack[A] = Kleisli { setting =>
      IO { f(setting) }
    }
    def filterSetting[A, B](s: Setting => B)(f: B => A): Stack[A] = Kleisli { setting =>
      IO { f(s(setting)) }
    }
  }

  // all types json codec
  object jsonCodec extends AccountJsonCodec
    with TokenJsonCodec
    with NodeJsonCodec

  object orm extends AccountSnapshotORM

  object handlers
      extends AccountServiceHandler.Implicits
      with CryptoServiceHandler.Implicits
      with LogServiceHandler.Implicits
      with MonitorServiceHandler.Implicits
      with NetworkServiceHandler.Implicits
      with AccountSnapshotHandler.Implicits
      with TransactionServiceHandler.Implicits
      with TransactionStoreHandler.Implicits
      with ConsensusEngineHandler.Implicits
      with ContractServiceHandler.Implicits
      with ContractStoreHandler.Implicits
      with LedgerStoreHandler.Implicits
      with NetworkStoreHandler.Implicits
      with bigknife.sop.effect.error.ErrorMInstance

  object runner {
    import bigknife.sop._, implicits._
    import fssi.ast.domain.components._
    import fssi.ast.domain.components.Model._
    import handlers._

    def runStack[A](p: SP[Model.Op, A]): Stack[A]                         = p.interpret[Stack]
    def runIO[A](p: SP[Model.Op, A], setting: Setting): cats.effect.IO[A] = runStack(p)(setting)
    def runIOAttempt[A](p: SP[Model.Op, A],
                        setting: Setting): cats.effect.IO[Either[Throwable, A]] =
      runStack(p)(setting).attempt

  }
}
