package fssi.interpreter

import fssi.ast.domain.components.Model.Op
import fssi.ast.usecase.Nymph
import org.scalatest._

class NymphSpec extends FunSuite {
  val nymph: Nymph[Op] = Nymph[Op]
  val setting: Setting = Setting()

  test("register") {
    info(s"$nymph")
    val register = nymph.register("hello,world")

    //assertThrows[NotImplementedError] {
      runner.runIOAttempt(register, setting).unsafeRunSync() match {
        case Left(t) => info(s"failed: ${t.getMessage}")
        case Right(account) => info(s"created account: $account")
      }
    //}
  }
}
