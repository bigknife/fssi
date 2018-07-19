package fssi.ast.usecase

import java.nio.file.Path

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

  override def createPublishContract(owner: String,
                                     privateKey: String,
                                     password: String,
                                     iv: String,
                                     name: String,
                                     version: String,
                                     contract: String): SP[F, Transaction] =
    for {
      pk                <- rebuildPriv(password, privateKey, iv)
      id                <- transactionService.randomTransactionID()
      contractNotHashed <- contractService.createContractWithoutHash(Account.ID(owner), name, version, contract)
      codeSign          <- cryptoService.makeSignature(contractNotHashed.toBeSigned, pk)
      contractHashed    <- contractNotHashed.copy(codeSign = Signature(codeSign)).pureSP[F]
      pubContractNotSigned <- transactionService.createPublishContractWithoutSign(id,
                                                                                  owner,
                                                                                  name,
                                                                                  version,
                                                                                  contractHashed)

      sign <- cryptoService.makeSignature(pubContractNotSigned.toBeVerified, pk)
    } yield pubContractNotSigned.copy(signature = Signature(sign.bytes)): Transaction

  override def createRunContract(owner: String,
                                 privateKey: String,
                                 password: String,
                                 iv: String,
                                 name: String,
                                 version: String,
                                 function: String,
                                 params: String): SP[F, Transaction] =
    for {
      pk         <- rebuildPriv(password, privateKey, iv)
      id         <- transactionService.randomTransactionID()
      parameter  <- contractService.createParameterFromString(params)
      legalParam <- err.either(parameter)
      runContractNotSigned <- transactionService.createRunContractWithoutSign(id,
                                                                              owner,
                                                                              name,
                                                                              version,
                                                                              function,
                                                                              legalParam)

      sign <- cryptoService.makeSignature(runContractNotSigned.toBeVerified, pk)
    } yield runContractNotSigned.copy(signature = Signature(sign.bytes)): Transaction

  override def compileContract(source: Path): SP[F, BytesValue] =
    for {
      classPathOr  <- contractService.compileContractSourceCode(source)
      classPath    <- err.either(classPathOr)
      determinedOr <- contractService.checkDeterministicOfClass(classPath)
      determined   <- err.either(determinedOr)
      path         <- contractService.jarContract(determined)
    } yield path

  private def rebuildPriv(password: String,
                          encryptedPrivateKey: String,
                          iv: String): SP[F, KeyPair.Priv] =
    for {
      key <- cryptoService.enforceDes3Key(BytesValue(password))
      pkValue <- cryptoService.des3cbcDecrypt(BytesValue.decodeHex(encryptedPrivateKey),
                                              key,
                                              BytesValue.decodeHex(iv))
      pk <- cryptoService.rebuildPriv(pkValue)
    } yield pk
}

object CmdTool {
  def apply[F[_]](implicit M: Model[F]): CmdTool[F] = new CmdTool[F] {
    override val model: Model[F] = M
  }
}
