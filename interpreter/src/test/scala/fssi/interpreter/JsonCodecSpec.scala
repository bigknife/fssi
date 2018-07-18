package fssi.interpreter

import bigknife.scalap.ast.types.{Envelope, Message, NodeID, QuorumSet}
import org.scalatest.FunSuite
import io.circe.parser._
import io.circe.syntax._
import fssi.interpreter.jsonCodec._
import fssi.ast.domain.types._
import fssi.interpreter.scp.QuorumSetSync

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
          case Left(t1)  => throw t1
          case Right(v1) => info(s"$v1")
        }
    }


  }

  test("Envelope[Message.Externalize]") {
    val externalizeMsg =
      "{\"statement\":{\"nodeID\":\"02a107e6206824925ff218add39dcdff99092b426bc79b071eb311edb22be426db\"," +
        "\"slotIndex\":\"02\",\"quorumSetHash\":\"7b06c40bd92c613af4f588a51a44a92439945a553563b7ee55fc6f7c110a7fab\"," +
        "\"message\":{\"commit\":{\"counter\":1,\"value\":[{\"oldStates\":{\"states\":{\"0281e3120890180ae38cda921a822" +
        "3c17f4db284ff43c9c31fed54e0dd6356c042\":{\"accountId\":\"0281e3120890180ae38cda921a8223c17f4db284ff43c9c31fed54" +
        "e0dd6356c042\",\"amount\":0,\"assets\":{\"banana\":\"rO0ABXNyABdtZS5iaWdrbmlmZS50ZXN0LkJhbmFuYZy5To9PNG++AgADR" +
        "AAFcHJpY2VEAAZ3ZWlnaHRMAARuYW1ldAASTGphdmEvbGFuZy9TdHJpbmc7eHA/4AAAAAAAAEBZAAAAAAAAdAADcGhp\"}},\"a\":{" +
        "\"accountId\":\"a\",\"amount\":0,\"assets\":{}},\"b\":{\"accountId\":\"b\",\"amount\":0,\"assets\":{}},\"c\":{" +
        "\"accountId\":\"c\",\"amount\":0,\"assets\":{}}}},\"transaction\":{\"type\":\"Transaction.InvokeContract\"," +
        "\"impl\":{\"id\":\"76e16b6872e2480887579316df687102\",\"invoker\":\"0281e3120890180ae38cda921a8223c17f4db284" +
        "ff43c9c31fed54e0dd6356c042\",\"name\":\"com.test\",\"version\":\"0.0.1\",\"function\":\"function1\",\"parameter" +
        "\":[\"phi\",\"0.5\",\"100\"],\"signature\":\"MEUCIBQLmcpQhYHzk/86YX6uzNs57z/rNyg8go91/uYaXjOlAiEAzaGHRt4hxmpdrQ" +
        "EwGdhdQ3BD6aPclutDJZHAoHf5L88=\",\"status\":\"Init:76e16b6872e2480887579316df687102\"}},\"newStates\":{" +
        "\"states\":{\"0281e3120890180ae38cda921a8223c17f4db284ff43c9c31fed54e0dd6356c042\":{\"accountId\":\"0281e312" +
        "0890180ae38cda921a8223c17f4db284ff43c9c31fed54e0dd6356c042\",\"amount\":0,\"assets\":{\"banana\":\"rO0ABX" +
        "NyABdtZS5iaWdrbmlmZS50ZXN0LkJhbmFuYZy5To9PNG++AgADRAAFcHJpY2VEAAZ3ZWlnaHRMAARuYW1ldAASTGphdmEvbGFuZy9TdHJpbmc" +
        "7eHA/4AAAAAAAAEBZAAAAAAAAdAADcGhp\"}},\"a\":{\"accountId\":\"a\",\"amount\":0,\"assets\":{}},\"b\":{\"accountId" +
        "\":\"b\",\"amount\":0,\"assets\":{}},\"c\":{\"accountId\":\"c\",\"amount\":0,\"assets\":{}}}},\"oldStatesHash\":" +
        "\"fc74c094d5bebfa5b59318dd9bdf6648617815039b7416c3eb57dec3d372beb9\",\"newStatesHash\":\"fc74c0" +
        "94d5bebfa5b59318dd9bdf6648617815039b7416c3eb57dec3d372beb9\",\"timestamp\":1531540558659}]},\"hCounter\":1}}," +
        "\"signature\":\"3046022100d07bff63c81f2e50a33c3c44e12d96e1b42a4d4c81411f7fbfa13c9be40010d9022100f0320e955c6ee" +
        "343b0e94e13872b2033737a932a9b827ea919ffed8be19f70f6\",\"type\":\"externalize\"} "
    parse(externalizeMsg) match {
      case Left(t) => throw t
      case Right(json) =>
        json.as[Envelope[Message]] match {
          case Left(t1) => throw t1
          case Right(m) => info(s"$m")
        }
    }
  }

  test("Map[NodeID, QuorumSet]") {
    val message = "[\n  {\n    \"nodeID\" : \"03d3e70862e399fc94ac62edd111398aa48f61de3a20d1ace979c02b3d9075e08a\",\n    \"qs\" : {\n      \"threshold\" : 3,\n      \"validators\" : [\n        \"03d3e70862e399fc94ac62edd111398aa48f61de3a20d1ace979c02b3d9075e08a\",\n        \"0281e3120890180ae38cda921a8223c17f4db284ff43c9c31fed54e0dd6356c042\",\n        \"0338984c81ba98e807dee4b1ff2079a77609a7ad8765054b7510df278ef5250f71\",\n        \"02a107e6206824925ff218add39dcdff99092b426bc79b071eb311edb22be426db\"\n      ]\n    }\n  }\n]"
    val ret= for {
      json <- parse(message)
      map <- json.as[Map[NodeID, QuorumSet]]
    } yield map

    println(ret)
  }

  test("QuorumSetSync") {
    val node1 = NodeID("v1".getBytes)
    val node2 = NodeID("v2".getBytes)
    val node3 = NodeID("v3".getBytes)
    val node4 = NodeID("v4".getBytes)
    val qs = QuorumSet.simple(3, node1, node2, node3, node4)
    val qs1 = qs.nest(2, node1, node4)

    val version: Long = 0
    val hash1 = QuorumSetSync.hash(version, Map(node1 -> qs, node2 -> qs, node3 -> qs1, node4 -> qs1))

    val hash2 = QuorumSetSync.hash(version, Map(node2 -> qs, node1 -> qs, node4 -> qs1, node3 -> qs1))

    assert(hash1 == hash2)

    val qss = QuorumSetSync(version, Map(node2 -> qs, node1 -> qs, node4 -> qs1, node3 -> qs1), hash1)

    val jso = qss.asJson
    info(jso.spaces2)

    for {
      str <- parse(jso.spaces2)
      qss1 <- str.as[QuorumSetSync]
    } yield {
      val hash3 = qss1.hash
      assert(hash1 == hash3)
      val hash4 = QuorumSetSync.hash(qss1.version, qss1.registeredQuorumSets)
      assert(hash4 == hash3)
    }


  }
}
