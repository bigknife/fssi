package fssi
package sandbox
import java.nio.file.Paths

import fssi.base.Base58
import fssi.types.biz.Account
import fssi.types.biz.Contract.UserContract.Parameter._
import fssi.types.biz.Contract.UserContract._
import fssi.utils._
import org.scalatest.FunSuite

class SandBoxTest extends FunSuite {

  crypto.registerBC()

  val sandBox = new SandBox

  val account         = "1o3LkGMAf6VajBCR479sgFP6xYenQUFgk"
  val publicKey       = "yydpyqKHPi1QDUgGrBrZvshqYNu4zQLoYwnM5qV1zWcU"
  val entryPrivateKey = "4NsYxMHaCFAT37Azud5nRmd3A1BSLC7eFuzBDJkK3mtfrWeAKd4ShWN"
  val key             = "DZzZ8NTFaeGYVAkUeKQ2kpqpJ51zpAQ2TLNSZnb59CLP"
  val iv              = "M9VGT9TcJ54"
  val privateKey =
    crypto.des3cbcDecrypt(Base58.decode(entryPrivateKey).get,
                          Base58.decode(key).get,
                          Base58.decode(iv).get)
  val accountId = Account.ID(Base58.decode(account).get)
  val pubKey    = Account.PubKey(Base58.decode(publicKey).get)
  val prvKey    = Account.PrivKey(privateKey)

  val account1 = "18yqbuuBHmp5gc5gwUUVk88JMRDDXbwbJp"

  val project    = "/tmp/fssi/contract"
  val projectDir = Paths.get(project)
  val output     = "/tmp/fssi/test_contract"
  val outputFile = Paths.get(output).toFile
  val version    = "1.0.0"

  test("test compile contract") {
    sandBox.compileContract(accountId, pubKey, prvKey, projectDir, version, outputFile) match {
      case Right(_) => println("SUCCESS: compile project success")
      case Left(e)  => e.printStackTrace()
    }
  }

  test("check contract determinism") {
    sandBox.checkContractDeterminism(pubKey, outputFile) match {
      case Right(_) => println("SUCCESS: check project success")
      case Left(e)  => e.printStackTrace()
    }
  }

  test("run smart contract") {
    val context    = new TestContext
    val methodName = "tokenQuery"
    val full       = "com.fssi.sample.InterfaceSample#tokenQuerySample(Context,String)"
    val parameter  = PString(account1)
    val r = for {
      contract <- sandBox.buildUnsignedContract(pubKey, outputFile)
      _        <- sandBox.executeContract(pubKey, context, contract, Method(methodName, full), parameter)
    } yield ()
    r match {
      case Right(_) => println("SUCCESS: run contract success")
      case Left(e)  => e.printStackTrace()
    }
  }

  test("test crypto") {
    val str   = "11111111122222222222233333333444444455565"
    val bytes = str.getBytes("utf-8")
    val signature =
      crypto.makeSignature(bytes, crypto.rebuildECPrivateKey(prvKey.value, crypto.SECP256K1))
    val verified = crypto.verifySignature(signature,
                                          bytes,
                                          crypto.rebuildECPublicKey(pubKey.value, crypto.SECP256K1))
    assert(verified)
  }
}
