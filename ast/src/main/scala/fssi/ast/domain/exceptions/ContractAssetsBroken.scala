package fssi.ast.domain.exceptions

case class ContractAssetsBroken(accountId: String)
    extends FSSIException(s"Contract Assets of Account(id=$accountId) Has Been Broken")
