package fssi
package tool
package handler

import types.biz._
import types.base._
import types.implicits._
import interpreter._
import types.syntax._
import ast._
import uc._
import io.circe._
import io.circe.syntax._
import jsonCodecs._
import java.io._
import java.nio.charset.Charset

trait CreateAccountHandler extends BaseHandler with LogSupport {


  def apply(randomSeed: String, accountFile: File, secretKeyFile: File): Unit = {
    val setting: Setting = Setting.ToolSetting()
    val (account, sk) = runner
      .runIO(toolProgram.createAccount(RandomSeed(randomSeed.getBytes("utf-8"))), setting)
      .unsafeRunSync
    saveAccount(account, accountFile)
    saveSecretKey(sk, secretKeyFile)

    log.info("Account Created.")

  }


  private def saveAccount(account: Account, f: File): Unit = {
    better.files.File(f.toPath).overwrite(account.asJson.spaces2)
    ()
  }


  private def saveSecretKey(sk: Account.SecretKey, f: File): Unit = {
    better.files.File(f.toPath).overwrite(sk.asBytesValue.bcBase58)
    ()
  }

}
