package fssi
package interpreter

import org.scalatest._

import types.biz._
import types.base._
import types.syntax._
import ast._, uc._

class AccountProgramSpec extends FunSuite with GivenWhenThen {

  val agentProgram = ToolProgram[components.Model.Op]
  val setting = Setting.DefaultSetting

  test("create account") {
    Given("a password: 88888888")
    val password = "88888888"

    val (account, sk) = runner.runIO(agentProgram.createAccount(RandomSeed(password.getBytes)), setting).unsafeRunSync
    info(s"created account is $account")
  }
}
