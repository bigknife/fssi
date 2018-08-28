package fssi
package tool
package object handler {
  object createAccount   extends CreateAccountHandler
  object createChain     extends CreateChainHandler
  object compileContract extends CompileContractHandler
  object runContract     extends RunContractHandler
}
