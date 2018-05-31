package fssi.ast.domain

import cats.Id
import fssi.ast.usecase.Nymph
import org.scalatest.FunSuite
import bigknife.sop.implicits._

class NymphSpec extends FunSuite {
  test("compile passed, and throw exception when running") {

    val nymph = Nymph[components.Model.Op]
    info(s"$nymph")

    val rand = "12345"

    import ModelHandler._
    import components.Model._

    assertThrows[scala.NotImplementedError] {
      nymph.register(rand).interpret[Id]
    }


  }
}
