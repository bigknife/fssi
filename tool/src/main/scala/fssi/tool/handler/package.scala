package fssi
package tool
package object handler {
  object createAccount                    extends CreateAccountHandler
  object createChain                      extends CreateChainHandler
  object createTransferTransaction        extends CreateTransferTransactionHandler
  object createPublishContractTransaction extends CreatePublishContractTransactionHandler
  object createRunContractTransaction     extends CreateRunContractTransactionHandler
  object compileContract                  extends CompileContractHandler

}
