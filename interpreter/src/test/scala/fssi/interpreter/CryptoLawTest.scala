package fssi
package interpreter
import fssi.ast.uc.law.CryptoLaw
import org.scalatest.FunSuite

class CryptoLawTest extends FunSuite {

  test("test crypto law program") {
    val cryptoLawProgram = CryptoLaw[ast.blockchain.Model.Op]
    val setting          = Setting.defaultInstance
    val success          = runner.runIO(cryptoLawProgram.privKeyEncryptAndDecrypt(), setting).unsafeRunSync()
    assert(success)
  }
}
