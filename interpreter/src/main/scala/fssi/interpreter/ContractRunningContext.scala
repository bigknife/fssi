package fssi
package interpreter

import contract.lib._

case class ContractRunningContext(sqlStore: SqlStore,
                                  kvStore: KVStore,
                                  tokenQuery: TokenQuery,
                                  currentAccountId: String)
    extends Context
