package fssi.store.bcs.types

import java.net.URLEncoder

sealed trait BCSKey {
  def persistedKey: Array[Byte]
  def snapshotKey: Array[Byte]
  def persistedKeyString = new String(persistedKey, "utf-8")
  def snapshotKeyString  = new String(snapshotKey, "utf-8")
}

object BCSKey {

  def parseFromSnapshot(bytes: Array[Byte]): Option[BCSKey] = {
    val str = new String(bytes, "utf-8")
    if (str.startsWith("meta:snapshot:")) {
      str.drop("meta:snapshot://".length) match {
        case "chainId" => Some(MetaKey.ChainID)
        case "height" => Some(MetaKey.Height)
        case "version" => Some(MetaKey.Version)
        case _ => None
      }
    }
    else None
  }

  private[types] trait BCSUrlKey extends BCSKey {
    val scheme: String
    val segments: Array[String]

    override def persistedKey: Array[Byte] = {
      val infix = "persisted"
      s"$scheme:$infix://${segments.map(URLEncoder.encode(_, "utf-8")).mkString("/")}"
        .getBytes("utf-8")
    }
    override def snapshotKey: Array[Byte] = {
      val infix = "snapshot"
      s"$scheme:$infix://${segments.map(URLEncoder.encode(_, "utf-8")).mkString("/")}"
        .getBytes("utf-8")
    }
  }
  sealed trait MetaKey extends BCSKey
  object MetaKey {
    val ChainID: MetaKey = new MetaKey with BCSUrlKey {
      override val scheme: String          = "meta"
      override val segments: Array[String] = Array("chainId")
    }
    val Height: MetaKey = new MetaKey with BCSUrlKey {
      override val scheme: String          = "meta"
      override val segments: Array[String] = Array("height")
    }
    val Version: MetaKey = new MetaKey with BCSUrlKey {
      override val scheme: String          = "meta"
      override val segments: Array[String] = Array("version")
    }
  }

}
