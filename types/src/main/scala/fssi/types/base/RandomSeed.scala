package fssi.types
package base

/** random seeds, used to create account encrypting key. the encryting key is for 
  * protecting the private key of the account.
  */
case class RandomSeed(value: Array[Byte])
