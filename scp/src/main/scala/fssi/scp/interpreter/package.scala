package fssi.scp

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
      extends NodeServiceHandler.Implicits
      with NodeStoreHandler.Implicits
      with ApplicationServiceHandler.Implicits
      with LogServiceHandler.Implicits
      with bigknife.sop.effect.error.ErrorMInstance

  object runner {
    import bigknife.sop._, implicits._
    import ast.components._
    import ast.components.Model._
    import handlers._

    def runStack[A](p: SP[Model.Op, A]): Stack[A] = p.interpret[Stack]

    def runIO[A](p: SP[Model.Op, A], setting: Setting): cats.effect.IO[A] = runStack(p)(setting)

    def runIOAttempt[A](p: SP[Model.Op, A],
                        setting: Setting): cats.effect.IO[Either[Throwable, A]] =
      runStack(p)(setting).attempt
  }
}
