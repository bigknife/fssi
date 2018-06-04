package fssi.interpreter

import fssi.ast.domain.components.Model.Op
import fssi.ast.usecase.Nymph
import org.scalatest._

class NymphSpec extends FunSuite with BeforeAndAfterAll {
  val nymph: Nymph[Op] = Nymph[Op]
  val setting: Setting = Setting()

  override protected def afterAll(): Unit = {
    /*
    runner.runIOAttempt(nymph.shutdown(), setting).unsafeRunSync() match {
      case Left(t) => t.printStackTrace()
      case Right(_) => ()
    }
    */
  }

  test("register") {
    info(s"$nymph")
    val p2p = nymph.startup("localhost", 9080, Vector.empty)

    val register = nymph.register("hello,world")

    val p = for {
      _ <- p2p
      x <- register
    } yield x

    val result = runner.runIOAttempt(p, setting).unsafeRunSync()
    if (result.isLeft) result.left.foreach(_.printStackTrace)
    assert(result.isRight)
    import io.circe.syntax._
    import fssi.interpreter.jsonCodec._

    info(result.right.get.asJson.spaces4)

  }
}
