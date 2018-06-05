package fssi.ast.usecase

import bigknife.sop._, implicits._
import fssi.ast.domain.components.Model
import fssi.ast.domain.types._

trait CmdTool[F[_]] extends CmdToolUseCases[F] {

  val model: Model[F]

  import model._

  /** use rand to create an account
    * the account info won't enter into the chain.
    */
  override def createAccount(rand: String): SP[F, Account] =
    for {
      kp          <- cryptoService.generateKeyPair()
      privData    <- cryptoService.privateKeyData(kp.priv)
      publData    <- cryptoService.publicKeyData(kp.publ)
      iv          <- cryptoService.randomByte(len = 8)
      pass        <- cryptoService.enforceDes3Key(BytesValue(rand))
      encPrivData <- cryptoService.des3cbcEncrypt(privData, pass, iv)
      uuid        <- cryptoService.randomUUID()
      acc         <- accountService.createAccount(publData, encPrivData, iv, uuid)
    } yield acc

  /** create a transfer transaction
    *
    */
  override def createTransfer(from: String,
                              to: String,
                              amount: Long,
                              privateKey: String,
                              password: String,
                              iv: String): SP[F, Transaction] =
    for {
      id                <- transactionService.randomTransactionID()
      transferNotSigned <- transactionService.createTransferWithoutSign(id, from, to, amount)
      key               <- cryptoService.enforceDes3Key(BytesValue(password))
      pkValue <- cryptoService.des3cbcDecrypt(BytesValue.decodeHex(privateKey),
                                              key,
                                              BytesValue.decodeHex(iv))
      pk   <- cryptoService.rebuildPriv(pkValue)
      sign <- cryptoService.makeSignature(transferNotSigned.toBeVerified, pk)
    } yield transferNotSigned.copy(signature = Signature(sign.bytes)): Transaction
}

object CmdTool {
  def apply[F[_]](implicit M: Model[F]): CmdTool[F] = new CmdTool[F] {
    override val model: Model[F] = M
  }
}
