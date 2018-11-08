package fssi.store.core

import jetbrains.exodus.env._

class XodusKVStore(storeName: String, environment: Environment, transaction: Option[Transaction])
    extends RouteXodusKVStore(environment, transaction) {
  override def routeStoreName(key: Array[Byte]): String = storeName

  override def routeStoreKey(key: Array[Byte]): Array[Byte] = key
}
