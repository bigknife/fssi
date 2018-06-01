package fssi.interpreter

import fssi.ast.domain.components.Model.Op
import fssi.ast.usecase.Nymph
import org.scalatest._

class NymphSpec extends FunSuite {
  val nymph: Nymph[Op] = Nymph[Op]
  val setting: Setting = Setting()

  ignore("register") {
    info(s"$nymph")
    val register = nymph.register("hello,world")

    val result = runner.runIOAttempt(register, setting).unsafeRunSync()
    if (result.isLeft) result.left.foreach(_.printStackTrace)
    assert(result.isRight)
  }
}
