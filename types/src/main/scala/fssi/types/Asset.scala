package fssi.types

/**
  * Digital assets
  * @param owner the owner's account public key a hex string.
  * @param name the unique name of asset
  */
case class Asset(
  owner: Account.ID,
  name: UniqueName,
  data: DataBlock.Index
)
