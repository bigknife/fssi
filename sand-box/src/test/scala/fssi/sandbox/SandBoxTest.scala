package fssi
package sandbox
import java.nio.file.Paths

import fssi.types.biz.Contract.UserContract._
import fssi.types.biz.Account
import org.scalatest.FunSuite
import Parameter._
import fssi.types.base.BytesValue
import fssi.utils._

class SandBoxTest extends FunSuite {

  crypto.registerBC()

  val sandBox = new SandBox

  test("test compile contract") {
    val account         = "12BFTJnXQYtiEEu42qqpqngVhntN1gSzcF"
    val publicKey       = "p4Dy6tfK5iJ1WMPUmqNYyTsWyszjpDCgMHaXYhD6JhtA"
    val entryPrivateKey = "ozdSkJVEDER2Tk12xTRpNeWvxxo9aHjusRauewpdPDcjFqGAXQsCakQ7HfjAQ8BTX"
    val key             = "BgpLjEqGFw3Zj7HK13gHns8MUXkEwkYpdQnhJHiS6kVw"
    val iv              = "BMzTqHsaPTtTZ26xEhmbQX"
    val privateKey =
      crypto.aesDecryptPrivKey(BytesValue.decodeBcBase58(iv).get.bytes,
                               BytesValue.decodeBcBase58(key).get.bytes,
                               BytesValue.decodeBcBase58(entryPrivateKey).get.bytes)
    val project    = "/Users/songwenchao/Documents/source/company/weihui/chain/fssi_contract"
    val output     = "/Users/songwenchao/fssi/banana.contract"
    val version    = "1.0.0"
    val projectDir = Paths.get(project)
    val outputFile = Paths.get(output).toFile
    val accountId  = Account.ID(BytesValue.decodeBcBase58(account).get.bytes)
    val pubKey     = Account.PubKey(BytesValue.decodeBcBase58(publicKey).get.bytes)
    val prvKey     = Account.PrivKey(privateKey)
    sandBox.compileContract(accountId, pubKey, prvKey, projectDir, version, outputFile) match {
      case Right(_) => println("SUCCESS: compile project success")
      case Left(e)  => e.printStackTrace()
    }
  }

  test("check contract determinism") {
    val output     = "/Users/songwenchao/fssi/banana.contract"
    val publicKey  = "p4Dy6tfK5iJ1WMPUmqNYyTsWyszjpDCgMHaXYhD6JhtA"
    val pubKey     = Account.PubKey(BytesValue.decodeBcBase58(publicKey).get.bytes)
    val outputFile = Paths.get(output).toFile
    sandBox.checkContractDeterminism(pubKey, outputFile) match {
      case Right(_) => println("SUCCESS: check project success")
      case Left(e)  => e.printStackTrace()
    }
  }

  test("run smart contract") {
    val publicKey  = "p4Dy6tfK5iJ1WMPUmqNYyTsWyszjpDCgMHaXYhD6JhtA"
    val pubKey     = Account.PubKey(publicKey.getBytes("utf-8"))
    val output     = "/Users/songwenchao/fssi/banana.contract"
    val outputFile = Paths.get(output).toFile
    val context    = new TestContext
    val methodName = "registerBanana"
    val full       = ""
    val parameter  = PArray(PString("hh"), PString("bbb"))
    sandBox.executeContract(pubKey, context, outputFile, Method(methodName, full), parameter) match {
      case Right(_) => println("SUCCESS: run contract success")
      case Left(e)  => e.printStackTrace()
    }
  }
}
