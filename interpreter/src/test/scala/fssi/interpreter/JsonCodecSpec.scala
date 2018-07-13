package fssi.interpreter

import bigknife.scalap.ast.types.{Envelope, Message}
import org.scalatest.FunSuite
import io.circe.parser._
import io.circe.syntax._
import fssi.interpreter.jsonCodec._
import fssi.ast.domain.types._

class JsonCodecSpec extends FunSuite {
  test("Contract.Parameter") {
    val s = "[\"name\",1]"

    val p = parse(s).map(_.as[Contract.Parameter])
    info(s"$p")

    val msg = "{\"statement\":{\"nodeID\":\"02a107e6206824925ff218add39dcdff99092b426bc79b071eb311edb22be426db\"," +
      "\"slotIndex\":\"01\",\"quorumSetHash\":\"7b06c40bd92c613af4f588a51a44a92439945a553563b7ee55fc6f7c110a7fab\"," +
      "\"message\":{\"ballot\":{\"counter\":1,\"value\":[{\"oldStates\":{\"states\":{" +
      "\"0281e3120890180ae38cda921a8223c17f4db284ff43c9c31fed54e0dd6356c042\":{\"accountId\":" +
      "\"0281e3120890180ae38cda921a8223c17f4db284ff43c9c31fed54e0dd6356c042\",\"amount\":0,\"assets\":{}}," +
      "\"a\":{\"accountId\":\"a\",\"amount\":0,\"assets\":{}},\"b\":{\"accountId\":\"b\",\"amount\":0,\"assets\":{}}," +
      "\"c\":{\"accountId\":\"c\",\"amount\":0,\"assets\":{}}}},\"transaction\":{\"type\":" +
      "\"Transaction.InvokeContract\",\"impl\":{\"id\":\"76e16b6872e2480887579316df687102\"," +
      "\"invoker\":\"0281e3120890180ae38cda921a8223c17f4db284ff43c9c31fed54e0dd6356c042\"," +
      "\"name\":\"com.test\",\"version\":\"0.0.1\",\"function\":\"function1\",\"parameter\":[" +
      "\"phi\",\"0.5\",\"100\"],\"signature\":\"MEUCIBQLmcpQhYHzk/86YX6uzNs57z/rNyg8go91/uYaXjOl" +
      "AiEAzaGHRt4hxmpdrQEwGdhdQ3BD6aPclutDJZHAoHf5L88=\",\"status\":\"Init:76e16b6872e2480887579316df687102\"}}," +
      "\"newStates\":{\"states\":{\"0281e3120890180ae38cda921a8223c17f4db284ff43c9c31fed54e0dd6356c042\":{" +
      "\"accountId\":\"0281e3120890180ae38cda921a8223c17f4db284ff43c9c31fed54e0dd6356c042\",\"amount\":0,\"assets\":{" +
      "\"banana\":\"rO0ABXNyABdtZS5iaWdrbmlmZS50ZXN0LkJhbmFuYZy5To9PNG++AgADRAAFcHJpY2VEAAZ3ZWlnaHRMAARuYW1ldAASTGphdm" +
      "EvbGFuZy9TdHJpbmc7eHA/4AAAAAAAAEBZAAAAAAAAdAADcGhp\"}},\"a\":{\"accountId\":\"a\",\"amount\":0,\"assets\":{}}," +
      "\"b\":{\"accountId\":\"b\",\"amount\":0,\"assets\":{}},\"c\":{\"accountId\":\"c\",\"amount\":0,\"assets\":{}}}}," +
      "\"oldStatesHash\":\"18ab69aadff5d7b2fb73b0d304b3bb58bbddae896def908e3b3ae91396e64d2d\",\"newStatesHash\":" +
      "\"fc74c094d5bebfa5b59318dd9bdf6648617815039b7416c3eb57dec3d372beb9\",\"timestamp\":1531494747687}]}," +
      "\"prepared\":{\"counter\":0,\"value\":\"\"},\"preparedPrime\":{\"counter\":0,\"value\":\"\"},\"hCounter\":0," +
      "\"cCounter\":0}},\"signature\":\"3045022100a2492801ad717270e180a856151f31d127fc20be607ffda29859ea2fbab4646a022" +
      "0343271751b626a2735b1e2285818b641b3670fa869a343403bf4733ef4deb0c8\",\"type\":\"prepare\"}"

    parse(msg) match {
      case Left(t) => throw t
      case Right(v) =>
        v.as[Envelope[Message]] match {
          case Left(t1) => throw t1
          case Right(v1) => info(s"$v1")
        }
    }
  }
}
