package fssi.interpreter

import java.nio.file.Paths

import fssi.ast.domain.types.Contract.Parameter.{PArray, PBigDecimal, PString}
import fssi.ast.domain.types._
import org.scalatest.{BeforeAndAfter, FunSuite}
import scala.language.reflectiveCalls

class LedgerStoreHandlerSpec extends FunSuite with BeforeAndAfter {
  def fixture = new {
    val setting: Setting = Setting(workingDir =
      Paths.get(System.getProperty("user.home"), s".fssi/${scala.util.Random.nextLong()}").toString)
    val ledgerStoreHandler = new LedgerStoreHandler
  }

  val f = fixture

  before {
    f.ledgerStoreHandler.init().run(f.setting).unsafeRunSync()
  }

  after {
    f.ledgerStoreHandler.clean()
  }

  test("load states for TransferContract") {
    val r = f.ledgerStoreHandler
      .loadStates(
        Account.ID("account_1"),
        Contract.inner.TransferContract,
        Some(PArray(PString("account_2"), PBigDecimal(10000)))
      )(f.setting)
      .attempt
      .unsafeRunSync()
    r match {
      case Left(t)  => t.getStackTrace.map(_.toString).foreach(x => info(x))
      case Right(x) => info(s"$x")
    }

    assert(r.isRight)
  }

  test("load states for PublishContract") {
    val r = f.ledgerStoreHandler
      .loadStates(
        Account.ID("account_1"),
        Contract.inner.PublishContract,
        Some(PArray(PString("account_2"), PBigDecimal(10000)))
      )(f.setting)
      .attempt
      .unsafeRunSync()
    r match {
      case Left(t)  => t.getStackTrace.map(_.toString).foreach(x => info(x))
      case Right(x) => info(s"$x")
    }
    assert(r.isRight)
  }

  test("load states for UserContract") {
    val contract: Contract.UserContract = Contract.UserContract(
      Account.ID(""),
      Contract.Name("com.test.test"),
      Contract.Version("0.0.1"),
      Contract.Code(
        "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"),
      Signature(BytesValue.decodeBase64(
        "MEUCIGI0zK5Ga1YOZF4johc/JFDSmzcvNvqI7feqAVaPlTWXAiEAphD0ZXC0si0Gzgk0lo5f2f2W0yRPC3yFlYln9UvJxGE="))
    )
    val r = f.ledgerStoreHandler
      .loadStates(
        Account.ID("account_1"),
        contract,
        None
      )(f.setting)
      .attempt
      .unsafeRunSync()
    r match {
      case Left(t)  => t.getStackTrace.map(_.toString).foreach(x => info(x))
      case Right(x) => info(s"$x")
    }
    assert(r.isRight)
  }
}
