package fssi
package tool
package handler

import types.biz._
import types.base._
import types.implicits._
import interpreter._
import ast._
import uc._
import io.circe._
import io.circe.syntax._
import jsonCodecs._
import java.io._
import java.nio.charset.Charset

trait CreateAccountToolProgram extends BaseToolProgram {
  def apply(randomSeed: String, accountFile: File, secretKeyFile: File): Effect = {
    for {
      pair <- toolProgram.createAccount(RandomSeed(randomSeed.getBytes("utf-8")))
      (account, sk) = pair
      _ <- saveAccount(account, accountFile)
      _ <- saveSecretKey(sk, secretKeyFile)
      _ <- logger.info("Account Created.")
    } yield ()
  }


  private def saveAccount(account: Account, f: File): Unit = {
    better.files.File(f.toPath).overwrite(account.asJson.spaces2)
  }


  private def saveSecretKey(sk: Account.SecretKey, f: File): Unit = {
    better.files.File(f.toPath).overwrite(sk.asBytesValue.bcBase58)
  }

}
