package fssi.types

/**
  * Account, is a crypto digital account id
  * @param publickey a hexstring
  * @param encryptedprivatekey encrypted hexstring
  */
case class Account(
    publicKey: HexString,
    encryptedPrivateKey: HexString,
    iv: HexString
) {
  val id: Account.ID = Account.ID(publicKey)
}

object Account {
  case class ID(value: HexString)
}
