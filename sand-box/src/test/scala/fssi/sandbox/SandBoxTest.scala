package fssi
package sandbox
import java.nio.file.Paths

import fssi.types.biz.Contract.UserContract.{Parameter, _}
import fssi.types.biz.Account
import org.scalatest.FunSuite
import Parameter._
import fssi.types.base.BytesValue
import fssi.utils._

class SandBoxTest extends FunSuite {

  crypto.registerBC()

  val sandBox = new SandBox

  val account         = "12BFTJnXQYtiEEu42qqpqngVhntN1gSzcF"
  val publicKey       = "p4Dy6tfK5iJ1WMPUmqNYyTsWyszjpDCgMHaXYhD6JhtA"
  val entryPrivateKey = "ozdSkJVEDER2Tk12xTRpNeWvxxo9aHjusRauewpdPDcjFqGAXQsCakQ7HfjAQ8BTX"
  val key             = "BgpLjEqGFw3Zj7HK13gHns8MUXkEwkYpdQnhJHiS6kVw"
  val iv              = "BMzTqHsaPTtTZ26xEhmbQX"
  val privateKey =
    crypto.aesDecryptPrivKey(BytesValue.decodeBcBase58(iv).get.bytes,
                             BytesValue.decodeBcBase58(key).get.bytes,
                             BytesValue.decodeBcBase58(entryPrivateKey).get.bytes)
  val accountId = Account.ID(BytesValue.decodeBcBase58(account).get.bytes)
  val pubKey    = Account.PubKey(BytesValue.decodeBcBase58(publicKey).get.bytes)
  val prvKey    = Account.PrivKey(privateKey)

  val project    = "/tmp/fssi_scaffold"
  val projectDir = Paths.get(project)
  val output     = "/tmp/banana.contract"
  val outputFile = Paths.get(output).toFile
  val version    = "1.0.0"

  ignore("test compile contract") {
    sandBox.compileContract(accountId, pubKey, prvKey, projectDir, version, outputFile) match {
      case Right(_) => println("SUCCESS: compile project success")
      case Left(e)  => e.printStackTrace()
    }
  }

  ignore("check contract determinism") {
    sandBox.checkContractDeterminism(pubKey, outputFile) match {
      case Right(_) => println("SUCCESS: check project success")
      case Left(e)  => e.printStackTrace()
    }
  }

  ignore("run smart contract") {
    val context    = new TestContext
    val methodName = "registerBanana"
    val full       = ""
    val parameter  = PArray(PString("hh"), PBigDecimal(123))
    sandBox.executeContract(pubKey, context, outputFile, Method(methodName, full), parameter) match {
      case Right(_) => println("SUCCESS: run contract success")
      case Left(e)  => e.printStackTrace()
    }
  }

  ignore("test crypto") {
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
