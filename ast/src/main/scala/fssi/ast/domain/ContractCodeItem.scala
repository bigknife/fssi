package fssi.ast.domain

/**
  * contract code item is an contract asset of an account
  * @param owner owner account, account public key, encoded in Hex
  * @param name contract name
  * @param version contract version
  * @param code contract code
  * @param sig contract code signature, encoded in base64
  */
case class ContractCodeItem(owner: String, name: String, version: String, code: String, sig: String)
