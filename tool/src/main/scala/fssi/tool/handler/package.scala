package fssi
package tool
package object handler {
  object createAccount             extends CreateAccountToolProgram
  object createChain               extends CreateChainToolProgram
  object createTransferTransaction extends CreateTransferTransactionToolProgram
  object createDeployTransaction   extends CreateDeployTransactionToolProgram
  object createRunTransaction      extends CreateRunContractTransactionToolProgram
  object compileContract           extends CompileContractToolProgram
  object createContractProject     extends CreateContractProjectToolProgram

}
