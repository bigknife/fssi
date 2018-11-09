package fssi.store

package object bcs {
  type TransactionKey  = types.TransactionKey
  type TransactionData = types.TransactionData

  type MetaKey  = types.BCSKey.MetaKey
  type MetaData = types.MetaData

  type ReceiptKey  = types.ReceiptKey
  type ReceiptData = types.ReceiptData

  type StateKey  = types.StateKey
  type StateData = types.StateData

}
